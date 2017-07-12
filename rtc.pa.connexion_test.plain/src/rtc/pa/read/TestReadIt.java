/*
 * Copyright (c) 2017 jeromede@fr.ibm.com
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

package rtc.pa.read;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.IFetchResult;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;

import rtc.utils.ProgressMonitor;

public class TestReadIt {

	public static String execute(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor)
			throws TeamRepositoryException, IOException {

		monitor.out(
				"Successfully connected to project area " + pa.getName() + " (process: " + pa.getProcessName() + ")");
		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		String result;
		result = readMembers(repo, pa, monitor);
		if (null != result)
			return result;
		result = readWorkItemTypes(repo, pa, wiClient, wiCommon, monitor);
		if (null != result)
			return result;
		return null;
	}

	private static String readContributors(IContributorHandle[] contributors, ITeamRepository repo, IProjectArea pa,
			ProgressMonitor monitor) {

		IContributor contrib;
		for (IContributorHandle contribHandle : contributors) {
			try {
				contrib = (IContributor) repo.itemManager().fetchCompleteItem(contribHandle, IItemManager.DEFAULT,
						monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error resolving IContributorHandle";
			}
			monitor.out("- " + contrib.getName() + " (" + contrib.getUserId() + ")");
		}
		return null;
	}

	private static String readMembers(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor) {

		String result;
		monitor.out("Its current administrators are:");
		result = readContributors(pa.getAdministrators(), repo, pa, monitor);
		if (null != result)
			return result;
		monitor.out("Its current members are (could have changed other time):");
		result = readContributors(pa.getMembers(), repo, pa, monitor);
		if (null != result)
			return result;
		return null;
	}

	public static String readWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor) {

		SortedMap<String, IWorkItemType> types = new TreeMap<String, IWorkItemType>();
		String message = null;
		monitor.out("Reading workflows...");
		IWorkflowInfo wf;
		List<IWorkItemType> allWorkItemTypes;
		try {
			allWorkItemTypes = wiClient.findWorkItemTypes(pa, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem while getting work item types";
		}
		for (IWorkItemType t : allWorkItemTypes) {
			types.put(t.getIdentifier(), t);
		}
		for (IWorkItemType t : types.values()) {
			monitor.out("\t" + t.getDisplayName() + " (" + t.getIdentifier() + ')');
			try {
				wf = wiCommon.getWorkflow(t.getIdentifier(), pa, monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "problem while getting workflow";
			}
			Identifier<IState>[] states = wf.getAllStateIds();
			for (Identifier<IState> state : states) {
				monitor.out("\t\tstate: " + state.getStringIdentifier());
				Identifier<IWorkflowAction>[] actions = wf.getActionIds(state);
				for (Identifier<IWorkflowAction> action : actions) {
					monitor.out("\t\t\taction: " + action.getStringIdentifier());
					Identifier<IState> result = wf.getActionResultState(action);
					monitor.out("\t\t\t\tto state: " + result.getStringIdentifier() + state.getStringIdentifier());
				}
			}
		}
		SortedMap<String, IAttribute> attributes;
		try {
			for (IWorkItemType t : types.values()) {
				monitor.out("Built in attributes for " + t.getIdentifier() + " (" + t.getDisplayName() + "):");
				List<IAttributeHandle> builtInAttributeHandles = wiCommon.findBuiltInAttributes(pa, monitor);
				IFetchResult builtIn = repo.itemManager().fetchCompleteItemsPermissionAware(builtInAttributeHandles,
						IItemManager.REFRESH, monitor);
				attributes = new TreeMap<String, IAttribute>();
				for (Object o : builtIn.getRetrievedItems()) {
					IAttribute a = (IAttribute) o;
					attributes.put(a.getIdentifier(), a);
				}
				for (IAttribute a : attributes.values()) {
					message = readAttribute(wiClient, monitor, a);
					if (null != message) {
						return message;
					}
				}
				monitor.out("Custom attributes for " + t.getIdentifier() + " (" + t.getDisplayName() + "):");
				List<IAttributeHandle> custAttributeHandles = t.getCustomAttributes();
				IFetchResult custom = repo.itemManager().fetchCompleteItemsPermissionAware(custAttributeHandles,
						IItemManager.REFRESH, monitor);
				attributes = new TreeMap<String, IAttribute>();
				for (Object o : custom.getRetrievedItems()) {
					if (o instanceof IAttribute) {
						IAttribute a = (IAttribute) o;
						attributes.put(a.getIdentifier(), a);
					}
				}
				for (IAttribute a : attributes.values()) {
					message = readAttribute(wiClient, monitor, a);
					if (null != message) {
						return message;
					}
				}
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem while getting work item type attributes";
		}
		return null;
	}

	private static String readAttribute(IWorkItemClient wiClient, ProgressMonitor monitor, IAttribute a) {
		monitor.out("\t" + a.getIdentifier() + " (" + a.getDisplayName() + ") : " + a.getAttributeType());
		ILiteral lit;
		ILiteral nullLit;
		if (AttributeTypes.isEnumerationAttributeType(a.getAttributeType())) {
			try {
				IEnumeration<? extends ILiteral> enumeration = wiClient.resolveEnumeration(a, monitor);
				nullLit = enumeration.findNullEnumerationLiteral();
				monitor.out("\t\tnull literal: " + ((null == nullLit) ? null : '"' + nullLit.getName() + '"'));
				for (Object o : enumeration.getEnumerationLiterals()) {
					if (o instanceof ILiteral) {
						lit = (ILiteral) o;
						monitor.out("\t\tliteral: " + lit.getName());
					}
				}
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error while reading enumeration";
			}
		}
		return null;
	}

}
