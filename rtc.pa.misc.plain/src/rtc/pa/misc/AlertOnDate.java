/*
 * Copyright (c) 2017,2018,2019 Jérôme Desquilbet <jeromede@fr.ibm.com>
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

package rtc.pa.misc;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.IFetchResult;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.Expression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;

import rtc.pa.utils.Login;
import rtc.pa.utils.ProgressMonitor;

public class AlertOnDate {

	private static class Flag {
		private String type_wi;
		private String date_attribute;
		private String flag_custom_attribute;
		private String flag_value;

		public Flag(String type_wi, String date_attribute, String flag_custom_attribute, String flag_value) {
			this.type_wi = type_wi;
			this.date_attribute = date_attribute;
			this.flag_custom_attribute = flag_custom_attribute;
			this.flag_value = flag_value;
		}

		public String getType_wi() {
			return type_wi;
		}

		public String getDate_attribute() {
			return date_attribute;
		}

		public String getFlag_custom_attribute() {
			return flag_custom_attribute;
		}

		public String getFlag_value() {
			return flag_value;
		}

		public String toString() {
			return type_wi + '|' + date_attribute + '|' + flag_custom_attribute + '|' + flag_value;
		}
	}

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		String message = null;

		String url, proj, user, password, year, month, day, hours, minutes, seconds;
		Timestamp time;
		String type_wi, date_attribute, flag_custom_attribute, flag_value;
		List<Flag> flags = new ArrayList<Flag>();
		int a = 0;
		URI uri;
		try {
			url = URI.create(args[a++]).toASCIIString();
			proj = new String(args[a++]);
			uri = URI.create(URLEncoder.encode(proj, StandardCharsets.US_ASCII.toString()).replaceAll("\\+", "%20"));
			user = new String(args[a++]);
			password = new String(args[a++]);
			year = new String(args[a++]);
			month = new String(args[a++]);
			day = new String(args[a++]);
			hours = new String(args[a++]);
			minutes = new String(args[a++]);
			seconds = new String(args[a++]);
			// yyyy-[m]m-[d]d hh:mm:ss
			time = Timestamp.valueOf(year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds);
			// {type_wi date_attribute flag_custom_attribute flag_value}+
			// Note: built-in date attributes are:
			// - com.ibm.team.workitem.attribute.creationdate
			// - com.ibm.team.workitem.attribute.duedate
			// - com.ibm.team.workitem.attribute.modified
			// - com.ibm.team.workitem.attribute.resolutiondate
			// - startDate
			while (a < args.length) {
				type_wi = new String(args[a++]);
				date_attribute = new String(args[a++]);
				flag_custom_attribute = new String(args[a++]);
				flag_value = new String(args[a++]);
				flags.add(new Flag(type_wi, date_attribute, flag_custom_attribute, flag_value));
			}
		} catch (Exception e) {
			monitor.err("arguments: ccm_url project_area_name user password year month day hours minutes seconds"
					+ " {type_wi date_attribute flag_custom_attribute flag_value}+");
			monitor.err("example: http://rtc.my.rational.com/ccm \"Training 3\" rational nopassword 2019 9 24 02 0 3"
					+ " ");
			monitor.err("bad args:");
			for (String arg : args) {
				monitor.err(' ' + arg);
			}
			monitor.err();
			return;
		}
		monitor.out("RTC server URL: " + url);
		monitor.out("Project name: " + proj + " (" + uri.toASCIIString() + ")");
		monitor.out("JazzProjectAdmin user ID: " + user);
		monitor.out("Password: " + "***");
		monitor.out("Date: " + time);
		for (Flag f : flags) {
			monitor.out("flag");
			monitor.out("\t" + f.getType_wi());
			monitor.out("\t" + f.getDate_attribute());
			monitor.out("\t" + f.getFlag_custom_attribute());
			monitor.out("\t" + f.getFlag_value());
		}
		TeamPlatform.startup();
		try {
			ITeamRepository repo = Login.login(url, user, password, monitor);
			IProcessClientService processClient = (IProcessClientService) repo
					.getClientLibrary(IProcessClientService.class);
			IProcessArea pa0 = (IProcessArea) (processClient.findProcessArea(uri, IProcessItemService.ALL_PROPERTIES,
					monitor));
			IProjectArea pa = null;
			if (null != pa0 && pa0 instanceof IProjectArea) {
				pa = (IProjectArea) pa0;
				execute(repo, pa, time, flags, monitor);
			} else {
				message = uri + " is not a project area";
			}
			if (null == message) {
				monitor.out("OK, done.");
			} else {
				monitor.out("KO: " + message);
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			monitor.out("Unable to perform: " + e.getMessage());
		} finally {
			TeamPlatform.shutdown();
		}
	}

	private static String execute(ITeamRepository repo, IProjectArea pa, Timestamp limit, List<Flag> flags,
			ProgressMonitor monitor) throws TeamRepositoryException {

		String message;
		IItemManager itemManager = repo.itemManager();
		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IWorkItemWorkingCopyManager wiCopier = wiClient.getWorkItemWorkingCopyManager();
		IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);

		IQueryClient queryClient = (IQueryClient) repo.getClientLibrary(IQueryClient.class);
		IQueryableAttribute attribute = null;
		attribute = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE).findAttribute(pa,
				IWorkItem.PROJECT_AREA_PROPERTY, auditableClient, monitor);
		Expression expression = new AttributeExpression(attribute, AttributeOperation.EQUALS, pa);
		IQueryResult<IResolvedResult<IWorkItem>> results = queryClient.getResolvedExpressionResults(pa, expression,
				IWorkItem.FULL_PROFILE);
		results.setLimit(Integer.MAX_VALUE);
		List<Flag> changes = new ArrayList<Flag>();
		IWorkItem wi;
		while (results.hasNext(monitor)) {
			changes.clear();
			wi = results.next(monitor).getItem();
			message = modifyWorkItem(wiCommon, wi,
					workItemChanges(wi, repo, pa, limit, flags, changes, wiClient, wiCommon, itemManager, monitor),
					wiCopier, itemManager, monitor);
			if (null != message)
				return message;
		}
		return null;
	}

	private static String modifyWorkItem(IWorkItemCommon wiCommon, IWorkItem wi, List<Flag> changes,
			IWorkItemWorkingCopyManager wiCopier, IItemManager itemManager, ProgressMonitor monitor)
			throws TeamRepositoryException {

		if (changes.isEmpty())
			return null;
		IWorkItemHandle wiH;
		WorkItemWorkingCopy wc;
		IWorkItem wic;
		wiH = (IWorkItemHandle) wi.getItemHandle();
		wiCopier.connect(wiH, IWorkItem.FULL_PROFILE, monitor);
		wc = wiCopier.getWorkingCopy(wiH);
		wic = wc.getWorkItem();
		//
		// Change custom attributes
		//
		List<IAttributeHandle> customAttributeHandles = wic.getCustomAttributes();
		IFetchResult custom = null;
		custom = itemManager.fetchCompleteItemsPermissionAware(customAttributeHandles, IItemManager.DEFAULT, monitor);
		IAttribute attribute;
		for (Object o : custom.getRetrievedItems()) {
			attribute = (IAttribute) o;
			for (Flag flag : changes) {
				if (flag.getFlag_custom_attribute().equals(attribute.getIdentifier())) {
					modifyAttribute(wiCommon, monitor, wic, attribute, flag.getFlag_value());
				}
			}
		}
		//
		// Save work item
		//
		IDetailedStatus detailedStatus = wc.save(monitor);
		if (!detailedStatus.isOK()) {
			return "error\n\t" + detailedStatus.getMessage() + "\nwhile changing version for work item " + wic.getId()
					+ " after changing some custom attributes";
		}
		return null;
	}

	private static void modifyAttribute(IWorkItemCommon wiCommon, ProgressMonitor monitor, IWorkItem wi,
			IAttribute attribute, String newValue) throws TeamRepositoryException {

		monitor.out(wi.getWorkItemType() + ' ' + wi.getId() + ' ' + attribute.getIdentifier() + ":"
				+ attribute.getAttributeType() + " = " + wi.getValue(attribute) + " --> " + newValue);

		Identifier<? extends ILiteral> newLiteralId;
		if (AttributeTypes.isEnumerationAttributeType(attribute.getAttributeType())) {
			newLiteralId = getLiteralId(wiCommon, monitor, attribute, newValue);
			if (null == newLiteralId) {
				monitor.out("no literal found in repository for (id) " + newValue);
			}
			wi.setValue(attribute, newLiteralId);
		} else {
			switch (AttributeTypes.getAttributeType(attribute.getAttributeType()).getIdentifier()) {
			case AttributeTypes.BOOLEAN:
				wi.setValue(attribute, new Boolean(newValue));
				break;
			case AttributeTypes.INTEGER:
				wi.setValue(attribute, new Integer(newValue));
				break;
			case AttributeTypes.SMALL_STRING:
			case AttributeTypes.MEDIUM_STRING:
			case AttributeTypes.LARGE_STRING:
				wi.setValue(attribute, newValue);
				break;
			default:
				monitor.out("this attribute type " + attribute.getAttributeType()
						+ " is not supported in the current version of this program"
						+ " (only custom enumerations, boolean, integer and string are supported)"
						+ ", skip attribute modification.");
				break;
			}
		}
	}

	private static Identifier<? extends ILiteral> getLiteralId(IWorkItemCommon wiCommon, ProgressMonitor monitor,
			IAttribute attribute, String literalIdentifier) throws TeamRepositoryException {

		IEnumeration<? extends ILiteral> enumeration = wiCommon.resolveEnumeration(attribute, monitor);
		ILiteral literal;
		for (Object o : enumeration.getEnumerationLiterals()) {
			literal = (ILiteral) o;
			if (0 == literal.getIdentifier2().getStringIdentifier().compareTo(literalIdentifier)) {
				return literal.getIdentifier2();
			}
		}
		return null;
	}

	private static List<Flag> workItemChanges(IWorkItem wi, ITeamRepository repo, IProjectArea pa, Timestamp limit,
			List<Flag> flags, List<Flag> changes, IWorkItemClient wiClient, IWorkItemCommon wiCommon,
			IItemManager itemManager, ProgressMonitor monitor) throws TeamRepositoryException {

		// Check built-in date attributes
		isLate(limit, wi.getCreationDate(), flags, changes, wi.getWorkItemType(),
				"com.ibm.team.workitem.attribute.creationdate");
		isLate(limit, wi.getDueDate(), flags, changes, wi.getWorkItemType(), "com.ibm.team.workitem.attribute.duedate");
		isLate(limit, new Timestamp(wi.modified().getTime()), flags, changes, wi.getWorkItemType(),
				"com.ibm.team.workitem.attribute.modified");
		isLate(limit, wi.getResolutionDate(), flags, changes, wi.getWorkItemType(),
				"com.ibm.team.workitem.attribute.resolutiondate");

		// Check custom date attributes
		IAttribute attribute;
		List<IAttributeHandle> customAttributeHandles = wi.getCustomAttributes();
		IFetchResult custom = null;
		custom = repo.itemManager().fetchCompleteItemsPermissionAware(customAttributeHandles, IItemManager.REFRESH,
				monitor);
		Object vObj;

		for (Object o : custom.getRetrievedItems()) {
			try {
				attribute = (IAttribute) o;
			} catch (Exception e) {
				continue;
			}
			vObj = wi.getValue(attribute);
			if (vObj instanceof Timestamp) {
				// this custom attribute's type is a date
				isLate(limit, (Timestamp) vObj, flags, changes, wi.getWorkItemType(), attribute.getIdentifier());
			}
		}
		return changes;
	}

	private static List<Flag> isLate(Timestamp limit, Timestamp date, List<Flag> flags, List<Flag> changes, String type,
			String attribute_id) {
		if (null == date) {
			return changes;
		}
		if (!date.after(limit)) {
			return changes;
		}
		for (Flag f : flags) {
			if (type.equals(f.getType_wi()) && attribute_id.equals(f.getDate_attribute())) {
				changes.add(f);
			}
		}
		return changes;
	}

}