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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.client.workingcopies.IWorkingCopyManager;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.common.IWorkItemCommon;

import rtc.pa.model.Category;
import rtc.pa.model.Member;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.utils.ProgressMonitor;

public class WriteIt {

	public static String execute(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p,
			Map<String, String> matchingUserIDs, String dir, String whenother)
			throws TeamRepositoryException, IOException {

		String message;

		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IWorkItemWorkingCopyManager wiCopier = wiClient.getWorkItemWorkingCopyManager();
		IProcessItemService service = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
		IWorkingCopyManager pCopier = service.getWorkingCopyManager();

		message = matchMembers(repo, pa, monitor, p, matchingUserIDs, whenother);
		if (null != message)
			return message;
		message = WorkItemTypeHelper.matchWorkItemTypes(repo, pa, wiClient, monitor, p);
		if (null != message)
			return message;
		message = writeCategories(repo, pa, wiCommon, monitor, p);
		if (null != message)
			return message;
		message = writeDevelopmentLines(repo, pa, service, pCopier, monitor, p);
		if (null != message)
			return message;
		message = writeWorkItems(repo, pa, wiClient, wiCommon, wiCopier, monitor, p, dir);
		if (null != message)
			return message;

		return null;
	}

	private static String matchMembers(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p,
			Map<String, String> matchingUserIDs, String whenother) {

		monitor.out("Matching user IDs:");
		for (String k : matchingUserIDs.keySet()) {
			monitor.out('\t' + k + " -> " + matchingUserIDs.get(k));
		}
		Map<String, IContributor> members = new HashMap<String, IContributor>();
		IContributor member;
		IContributor whenother_member = null;
		for (IContributorHandle contribHandle : pa.getMembers()) {
			try {
				member = (IContributor) repo.itemManager().fetchCompleteItem(contribHandle, IItemManager.DEFAULT,
						monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error resolving IContributorHandle";
			}
			if (member.getUserId().equals(whenother)) {
				whenother_member = member;
			}
			members.put(member.getUserId(), member);
		}
		// Add special unassigned
		IContributor unassigned = null;
		try {
			unassigned = repo.contributorManager().fetchContributorByUserId("unassigned", monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			unassigned = null;
		}
		if (null != unassigned) {
			members.put(unassigned.getUserId(), unassigned);
		}
		String oldId, newId;
		for (Member m : p.getMembers()) {
			member = null;
			oldId = m.getUserId();
			newId = matchingUserIDs.get(oldId);
			if (null == newId) {
				if (null == whenother_member) {
					return "user ID \"" + oldId + "\" was in the source RTC project area (or has been at some point),"
							+ " but it has not been found in the user matching file";
				} else {
					monitor.out(
							"user ID \"" + oldId + "\" was in the source RTC project area (or has been at some point),"
									+ " but it has not been found in the user matching file; it is replaced by: "
									+ whenother_member.getUserId());
					member = whenother_member;
				}
			} else {
				member = members.get(newId);
			}
			if (null == member) {
				return "user ID \"" + newId + "\" (before: \"" + oldId
						+ "\") has not been found in the target project area";
			}
			m.setExternalObject(member.getUserId(), member);
			monitor.out("User \"" + oldId + "\" (\"" + m.getName() + "\") is now \"" + member.getUserId() + "\" (\""
					+ member.getName() + "\")");
		}
		return null;
	}

	private static String writeCategories(ITeamRepository repo, IProjectArea pa, IWorkItemCommon wiCommon,
			ProgressMonitor monitor, Project p) {

		String message;
		for (Category cat : p.getCategories()) {
			message = CategoryBuilder.createCategory(pa, wiCommon, monitor, p, cat);
			if (null != message) {
				return "error creating category: " + message;
			}
		}
		return null;
	}

	private static String writeDevelopmentLines(ITeamRepository repo, IProjectArea pa, IProcessItemService service,
			IWorkingCopyManager pCopier, ProgressMonitor monitor, Project p) {

		try {
			TimelineBuilder.createTimelines(repo, pa, service, pCopier, monitor, p);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error while creating development lines and iterations";
		}
		return null;
	}

	private static String writeWorkItems(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			String dir) {

		String result;
		for (Task t : p.getTasks()) {
			result = WorkItemBuilder.createMinimalWorkItemWithLinks(repo, pa, wiClient, wiCommon, wiCopier, monitor, p,
					t, dir);
			if (null != result) {
				return "error creating minimal work item with links, etc. " + t.getId() + " (id in source): " + result;
			}
		}
		Map<String, String> tasks = new HashMap<String, String>();
		for (Task task : p.getTasks()) {
			tasks.put("" + task.getId(), task.getExternalId());
		}
		for (Task t : p.getTasks()) {
			result = WorkItemBuilder.createUpdateWorkItemWithAllVersions(repo, pa, wiClient, wiCommon, wiCopier,
					monitor, tasks, p, t);
			if (null != result) {
				return "error creating/updating versions for work item " + t.getId() + " (id in source): " + result;
			}
		}
		return null;
	}

}
