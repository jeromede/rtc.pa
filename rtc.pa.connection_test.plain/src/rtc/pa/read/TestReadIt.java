/*
 * Copyright (c) 2017,2018,2019 jeromede@fr.ibm.com
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ibm.team.links.common.registry.IEndPointDescriptor;
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
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;

import rtc.pa.utils.ProgressMonitor;

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
		result = readLinkTypes(repo, monitor);
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
			monitor.out(
					"- " + contrib.getUserId() + " (" + contrib.getName() + ") " + contrib.getItemId().getUuidValue());
		}
		return null;
	}

	private static String readMembers(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor) {

		String result;
		monitor.out("Its current administrators are:");
		result = readContributors(pa.getAdministrators(), repo, pa, monitor);
		if (null != result)
			return result;
		monitor.out("Its current members are (could have changed over time):");
		result = readContributors(pa.getMembers(), repo, pa, monitor);
		if (null != result)
			return result;
		IContributor unassigned = null;
		try {
			unassigned = repo.contributorManager().fetchContributorByUserId("unassigned", monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			unassigned = null;
		}
		if (null != unassigned) {
			monitor.out("- " + unassigned.getUserId() + " (" + unassigned.getName() + ") "
					+ unassigned.getItemId().getUuidValue());
		}
		return null;
	}

	public static String readWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor) {

		SortedMap<String, IWorkItemType> types = new TreeMap<String, IWorkItemType>();
		String result;
		monitor.out("Reading workflows...");
		IWorkflowInfo wf;
		List<IWorkItemType> allWorkItemTypes;
		SortedMap<String, Identifier<IState>> states;
		SortedMap<String, Identifier<IWorkflowAction>> actions;
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
			monitor.out("\t" + t.getIdentifier() + " (" + t.getDisplayName() + ')');
			try {
				wf = wiCommon.getWorkflow(t.getIdentifier(), pa, monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "problem while getting workflow";
			}
			states = new TreeMap<String, Identifier<IState>>();
			for (Identifier<IState> state : wf.getAllStateIds()) {
				states.put(state.getStringIdentifier(), state);
			}
			for (Identifier<IState> state: states.values()) {
				monitor.out("\t\tstate: " + state.getStringIdentifier() + " (" + wf.getStateName(state) + ")");
				actions = new TreeMap<String, Identifier<IWorkflowAction>>();
				for (Identifier<IWorkflowAction> action : wf.getActionIds(state)) {
					actions.put(action.getStringIdentifier(), action);
				}
				for (Identifier<IWorkflowAction> action : actions.values()) {
					monitor.out(
							"\t\t\taction: " + action.getStringIdentifier() + " (" + wf.getActionName(action) + ")");
					Identifier<IState> resultState = wf.getActionResultState(action);
					monitor.out("\t\t\t\tto state: " + resultState.getStringIdentifier() + " (" + wf.getStateName(state)
							+ ")");
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
					result = readAttribute(wiClient, monitor, a);
					if (null != result) {
						return result;
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
					result = readAttribute(wiClient, monitor, a);
					if (null != result) {
						return result;
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

	@SuppressWarnings("serial")
	private static final Map<String, IEndPointDescriptor> linkTypes = Collections
			.unmodifiableMap(new HashMap<String, IEndPointDescriptor>() {
				{
					put(WorkItemLinkTypes.BLOCKS_WORK_ITEM, WorkItemEndPoints.BLOCKS_WORK_ITEM);
					put(WorkItemLinkTypes.COPIED_WORK_ITEM, WorkItemEndPoints.COPIED_WORK_ITEM);
					put(WorkItemLinkTypes.DUPLICATE_WORK_ITEM, WorkItemEndPoints.DUPLICATE_WORK_ITEM);
					put(WorkItemLinkTypes.PARENT_WORK_ITEM, WorkItemEndPoints.PARENT_WORK_ITEM);
					put(WorkItemLinkTypes.RELATED_WORK_ITEM, WorkItemEndPoints.RESOLVES_WORK_ITEM);
					put(WorkItemLinkTypes.MENTIONS, WorkItemEndPoints.MENTIONS);
				}
			});

	private static String readLinkTypes(ITeamRepository repo, ProgressMonitor monitor) {
		monitor.out("Reading link types...");
		IEndPointDescriptor wiep;
		for (String wilt : linkTypes.keySet()) {
			wiep = linkTypes.get(wilt);
			monitor.out("\t" + wilt + " | " + wiep.getId());
		}
		return null;
	}

}
