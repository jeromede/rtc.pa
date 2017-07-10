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

package rtc.pa.write.plain;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;

import rtc.model.Category;
import rtc.model.Iteration;
import rtc.model.Line;
import rtc.model.Member;
import rtc.model.Project;
import rtc.utils.ProgressMonitor;

public class WriteIt {

	public static String execute(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p,
			Map<String, String> matchingUserIDs) throws TeamRepositoryException, IOException {

		String result;

		IItemManager itemManager = repo.itemManager();
		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IWorkItemWorkingCopyManager wiCopier = wiClient.getWorkItemWorkingCopyManager();
		IProcessItemService service = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
		IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);

		result = matchMembers(repo, pa, monitor, p, matchingUserIDs);
		if (null != result)
			return result;
		// result = writeCategories(repo, pa, wiCommon, monitor, p);
		// if (null != result)
		// return result;
		// result = writeDevelopmentLines(repo, pa, service, monitor, p);
		// if (null != result)
		// return result;
		result = writeWorkItems(repo, pa, wiClient, wiCommon, wiCopier, monitor, p);
		if (null != result)
			return result;

		return null;
	}

	private static String matchMembers(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p,
			Map<String, String> matchingUserIDs) {

		monitor.out("Matching user IDs:");
		for (String k : matchingUserIDs.keySet()) {
			monitor.out('\t' + k + " -> " + matchingUserIDs.get(k));
		}
		Map<String, IContributor> members = new HashMap<String, IContributor>();
		IContributor member;
		for (IContributorHandle contribHandle : pa.getMembers()) {
			try {
				member = (IContributor) repo.itemManager().fetchCompleteItem(contribHandle, IItemManager.DEFAULT,
						monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error resolving IContributorHandle";
			}
			members.put(member.getUserId(), member);
		}
		String oldId, newId;
		for (Member m : p.getMembers()) {
			oldId = m.getUserId();
			newId = matchingUserIDs.get(oldId);
			if (null == newId) {
				return "userID \"" + m.getUserId()
						+ "\" was in the source RTC, but it has not been found in the user matching file";
			}
			member = members.get(newId);
			if (null == member) {
				return "userID\" + newId \"" + m.getUserId() + "\" has not been found in the target project area";
			}
			m.setTargetObject(member.getUserId(), member);
			monitor.out("User \"" + oldId + "\" (\"" + m.getName() + "\") is now \"" + member.getUserId() + "\" (\""
					+ member.getName() + "\")");
		}
		return null;
	}

	private static String writeCategories(ITeamRepository repo, IProjectArea pa, IWorkItemCommon wiCommon,
			ProgressMonitor monitor, Project p) {

		String message;
		for (Category cat : p.getCategories()) {
			message = WriteHelper.createCategory(pa, wiCommon, monitor, p, cat);
			if (null != message) {
				return "error creating category: " + message;
			}
		}
		return null;
	}

	private static String writeDevelopmentLines(ITeamRepository repo, IProjectArea pa, IProcessItemService service,
			ProgressMonitor monitor, Project p) {

		String message;
		for (Line line : p.getLines()) {
			message = WriteHelper.createLine(pa, service, monitor, p, line);
			if (null != message) {
				return "error creating line: " + message;
			}
			for (Iteration ite : line.getIterations()) {
				message = WriteHelper.createIteration(pa, service, monitor, p, line, null, ite);
				if (null != message) {
					return "error creating iteration: " + message;
				}
			}
			message = WriteHelper.setLineCurrent(pa, service, monitor, line);
			if (null != message) {
				return "error setting current iteration in line: " + message;
			}
		}
		return null;
	}

	private static String writeWorkItems(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p) {

		//
		// Type
		//
		List<IWorkItemType> all;
		IWorkItemType wiType = null;
		try {
			wiType = wiClient.findWorkItemType(pa, "com.ibm.team.apt.workItemType.story", monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while retrieving work item types.");
		}
		if (null == wiType) {
			return ("Error: can't find work item type.");
		}
		//
		// Category
		//
		List<ICategory> findCategories;
		try {
			findCategories = wiClient.findCategories(pa, ICategory.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while retrieving categories.");
		}
		ICategory category = findCategories.get(0);
		//
		// Creation
		//
		IWorkItemHandle wiHandle;
		try {
			wiHandle = wiCopier.connectNew(wiType, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while creating work item.");
		}
		WorkItemWorkingCopy wc = wiCopier.getWorkingCopy(wiHandle);
		IWorkItem wi = wc.getWorkItem();
		IDetailedStatus s;
		try {
			wi.setCategory(category);
			wi.setCreator(repo.loggedInContributor());
			wi.setOwner(repo.loggedInContributor());
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10"));
			Timestamp t = new Timestamp(1098480784979L);
			wi.setCreationDate(t);
			List<IAttributeHandle> customAttributes = wi.getCustomAttributes();
			for (IAttributeHandle a : customAttributes) {
				System.out.println("custom attribute " + a.toString());
			}
			// IAttribute tpModified;
			// try {
			// tpModified = wiClient.findAttribute(pa, "tp_modified", monitor);
			// } catch (TeamRepositoryException e) {
			// e.printStackTrace();
			// return ("Can't find custom attribute tp_modified.");
			// }
			// System.out.println("custom attribute tp_modified " +
			// tpModified.toString());
			// wi.setValue(tpModified, t);
			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("Error saving new work item");
			}
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10 updated"));
			wi.setRequestedModified(t);
			s = wc.save(monitor);
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10 updated"));
			wi.setRequestedModified(t);
			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("Error updating new work item");
			}
		} finally {
			wiCopier.disconnect(wi);
		}
		try {
			wi = (IWorkItem) repo.itemManager().fetchCompleteItem(wi, IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error fetching new work item.");
		}
		int iwId = wi.getId();
		System.out.println("Created workitem: " + iwId);
		//
		// Update
		//
		try {
			wi = wiClient.findWorkItemById(iwId, IWorkItem.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Can't find work item " + iwId);
		}
		try {
			wiCopier.connect(wi, IWorkItem.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while connecting to work item.");
		}
		wc = wiCopier.getWorkingCopy(wi);
		wi = wc.getWorkItem();
		try {
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10 modified"));
			s = wc.save(monitor);
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10 modified"));
			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("Error saving updated work item");
			}
		} finally {
			wiClient.getWorkItemWorkingCopyManager().disconnect(wi);
		}
		//
		// Same Update
		//
		try {
			wi = wiClient.findWorkItemById(iwId, IWorkItem.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Can't find work item " + iwId);
		}
		try {
			wiCopier.connect(wi, IWorkItem.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while connecting to work item.");
		}
		wc = wiCopier.getWorkingCopy(wi);
		wi = wc.getWorkItem();
		try {
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10 modified"));
			s = wc.save(monitor);
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10 modified"));
			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("Error saving updated work item");
			}
		} finally {
			wiClient.getWorkItemWorkingCopyManager().disconnect(wi);
		}

		return null;
	}

}
