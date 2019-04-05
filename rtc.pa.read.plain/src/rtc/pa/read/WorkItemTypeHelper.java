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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IFetchResult;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItemType;

import rtc.pa.model.Attribute;
import rtc.pa.model.Literal;
import rtc.pa.model.Project;
import rtc.pa.model.TaskType;
import rtc.pa.utils.ProgressMonitor;

public class WorkItemTypeHelper {

	static String readWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p) {

		String result;
		monitor.out("Now reading work item types...");
		List<IWorkItemType> allWorkItemTypes;
		TaskType tt;
		try {
			allWorkItemTypes = wiClient.findWorkItemTypes(pa, monitor);
			for (IWorkItemType t : allWorkItemTypes) {
				tt = new TaskType(t.getIdentifier(), t.getDisplayName());
				p.putTaskType(tt);
				monitor.out("\t" + t.getDisplayName() + " (" + t.getIdentifier() + ')');
				result = addAttributes(repo, pa, wiClient, wiCommon, monitor, p, t, tt);
				if (null != result)
					return result;
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem while getting work item types";
		}
		return null;

	}

	private static String addAttributes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p, IWorkItemType t, TaskType tt) {

		String result;
		monitor.out("\t\tcustom attributes for " + t.getIdentifier() + " (" + t.getDisplayName() + "):");
		List<IAttributeHandle> customAttributeHandles = t.getCustomAttributes();
		IFetchResult custom = null;
		try {
			custom = repo.itemManager().fetchCompleteItemsPermissionAware(customAttributeHandles, IItemManager.REFRESH,
					monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error finding custom attributes for type " + t.getIdentifier();
		}
		for (Object o : custom.getRetrievedItems()) {
			if (o instanceof IAttribute) {
				result = addAttribute(wiClient, monitor, p, tt, (IAttribute) o);
				if (null != result) {
					return result;
				}
			}
		}
		return null;
	}

	private static String addAttribute(IWorkItemClient wiClient, ProgressMonitor monitor, Project p, TaskType tt,
			IAttribute attribute) {

		Attribute a;
		Literal lit;
		Collection<Literal> lits = null;
		Literal nullLit = null;
		ILiteral literal;
		ILiteral nullLiteral;
		if (AttributeTypes.isEnumerationAttributeType(attribute.getAttributeType())) {
			lits = new ArrayList<Literal>();
			try {
				IEnumeration<? extends ILiteral> enumeration = wiClient.resolveEnumeration(attribute, monitor);
				nullLiteral = enumeration.findNullEnumerationLiteral();
				monitor.out("\t\t\t\tnull literal: " + ((null == nullLiteral) ? null
						: nullLiteral.getIdentifier2().getStringIdentifier() + " (" + nullLiteral.getName() + ")"));
				if (null != nullLiteral) {
					nullLit = new Literal(nullLiteral.getIdentifier2().getStringIdentifier(), nullLiteral.getName());
					nullLit.setExternalObject(nullLiteral.getName(), nullLiteral);
				}
				for (Object o : enumeration.getEnumerationLiterals()) {
					if (o instanceof ILiteral) {
						literal = (ILiteral) o;
						monitor.out("\t\t\t\tliteral: " + literal.getIdentifier2().getStringIdentifier() + " ("
								+ literal.getName() + ")");
						lit = new Literal(literal.getIdentifier2().getStringIdentifier(), literal.getName());
						lit.setExternalObject(literal.getName(), lit); // TODO: itself??? Couldn't it be null instead?
						lits.add(lit);
					}
				}
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error while reading enumeration";
			}
		}
		monitor.out("attribute.getDisplayName(): " + attribute.getDisplayName());
		if (null == lits) {
			a = new Attribute(attribute.getIdentifier(), attribute.getDisplayName(), attribute.getAttributeType());
		} else {
			a = new Attribute(attribute.getIdentifier(), attribute.getDisplayName(), attribute.getAttributeType(), lits,
					nullLit);
		}
		a.setExternalObject(attribute.getIdentifier(), attribute);
		p.putAttribute(tt, a);
		return null;
	}

}
