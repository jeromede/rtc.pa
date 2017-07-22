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

package rtc.pa.write;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IFetchResult;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
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

	static String matchWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			ProgressMonitor monitor, //
			Project p) {

		String result;
		monitor.out("The workitem types are:");
		String typeId;
		IWorkItemType type;
		String attrId;
		IAttribute attribute;
		String litId;
		ILiteral literal;
		Map<String, IWorkItemType> typesMap = new HashMap<String, IWorkItemType>();
		Map<String, IAttribute> attributesMap = new HashMap<String, IAttribute>();
		Map<String, ILiteral> literalsMap = new HashMap<String, ILiteral>();
		result = getWorkItemTypes(repo, pa, wiClient, monitor, typesMap);
		if (null != result) {
			return result;
		}
		for (TaskType tt : p.getTaskTypes()) {
			typeId = tt.getSourceId();
			type = typesMap.get(typeId);
			if (null == type) {
				return "can't find work item type \"" + typeId + "\" in target project";
			}
			tt.setExternalObject(typeId, type);
			attributesMap = new HashMap<String, IAttribute>();
			result = getCustomAttributes(repo, monitor, type, attributesMap);
			if (null != result) {
				return "problem while getting custom attributes for work item type " + typeId + ": " + result;
			}
			for (Attribute attr : tt.getAttributes()) {
				attrId = attr.getSourceId();
				attribute = attributesMap.get(attrId);
				if (null == attribute) {
					return "can't find attribute " + attrId + " in type " + typeId;
				}
				attr.setExternalObject(attrId, attribute);
				if (attr.isEnum()) {
					if (!AttributeTypes.isEnumerationAttributeType(typeId)) {
						return "Work item type " + typeId + //
								" was a enumeration in the source project area "
								+ "but is not anymore in the target project area";
					}
					result = getLiterals(repo, wiClient, monitor, attribute, literalsMap);
					if (null != result) {
						return result;
					}
					for (Literal lit : attr.getLiterals()) {
						litId = lit.getSourceId();
						literal = literalsMap.get(litId);
						if (null == literal) {
							return "can't find literal " + litId + " for attribute " + attrId + " in type " + typeId;
						}
						lit.setExternalObject(litId, literal.getIdentifier2());
					}
					literal = getNullLiteral(repo, wiClient, monitor, attribute);
					if (null == literal) {
						if (null != attr.getNullLiteral()) {
							return "null literal for attribute " + attrId + " in type " + typeId
									+ " was null, it is not anymore";
						}
					} else {
						attr.getNullLiteral().setExternalObject(literal.getIdentifier2().getStringIdentifier(),
								literal);
					}
				}
			}
			monitor.out("\t" + type.getDisplayName() + " (" + typeId + ')');
		}
		return null;
	}

	private static String getWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			ProgressMonitor monitor, //
			Map<String, IWorkItemType> map) {

		List<IWorkItemType> allWorkItemTypes;
		try {
			allWorkItemTypes = wiClient.findWorkItemTypes(pa, monitor);
			for (IWorkItemType type : allWorkItemTypes) {
				map.put(type.getIdentifier(), type);
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem while getting work item types";
		}
		return null;
	}

	private static String getCustomAttributes(ITeamRepository repo, ProgressMonitor monitor, //
			IWorkItemType type, Map<String, IAttribute> map) {

		monitor.out("\tcustom attributes for " + type.getIdentifier() + " (" + type.getDisplayName() + "):");
		List<IAttributeHandle> customAttributeHandles = type.getCustomAttributes();
		IFetchResult custom = null;
		IAttribute attribute;
		try {
			custom = repo.itemManager().fetchCompleteItemsPermissionAware(customAttributeHandles, IItemManager.REFRESH,
					monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error finding custom attributes for type " + type.getIdentifier();
		}
		for (Object o : custom.getRetrievedItems()) {
			if (o instanceof IAttribute) {
				attribute = (IAttribute) o;
				map.put(attribute.getIdentifier(), attribute);
			}
		}
		return null;

	}

	private static String getLiterals(ITeamRepository repo, IWorkItemClient wiClient, ProgressMonitor monitor, //
			IAttribute attribute, Map<String, ILiteral> map) {

		monitor.out("\t\tliterals for " + attribute.getIdentifier() + " (" + attribute.getDisplayName() + "):");
		try {
			IEnumeration<? extends ILiteral> enumeration = wiClient.resolveEnumeration(attribute, monitor);
			ILiteral literal;
			for (Object o : enumeration.getEnumerationLiterals()) {
				if (o instanceof ILiteral) {
					literal = (ILiteral) o;
					monitor.out("\t\t\t\tliteral: " + literal.getIdentifier2().getStringIdentifier() + " ("
							+ literal.getName() + ")");
					map.put(literal.getIdentifier2().getStringIdentifier(), literal);
				}
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error while reading enumeration";
		}
		return null;
	}

	private static ILiteral getNullLiteral(ITeamRepository repo, IWorkItemClient wiClient, ProgressMonitor monitor, //
			IAttribute attribute) {

		ILiteral nullLiteral = null;
		IEnumeration<? extends ILiteral> enumeration;
		try {
			enumeration = wiClient.resolveEnumeration(attribute, monitor);
			nullLiteral = enumeration.findNullEnumerationLiteral();
			monitor.out("\t\t\t\tnull literal: " + ((null == nullLiteral) ? null
					: nullLiteral.getIdentifier2().getStringIdentifier() + " (" + nullLiteral.getName() + ")"));
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
		}
		return nullLiteral;
	}

}
