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
import java.util.List;
import java.util.Map;

import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IWorkItemType;

import rtc.pa.model.Category;
import rtc.pa.model.Iteration;
import rtc.pa.model.Line;
import rtc.pa.model.Member;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskType;
import rtc.pa.utils.ProgressMonitor;

public class WriteIt {

	public static String execute(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p,
			Map<String, String> matchingUserIDs) throws TeamRepositoryException, IOException {

		String message;

		// IItemManager itemManager = repo.itemManager();
		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IWorkItemWorkingCopyManager wiCopier = wiClient.getWorkItemWorkingCopyManager();
		IProcessItemService service = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
		// IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);

		message = matchMembers(repo, pa, monitor, p, matchingUserIDs);
		if (null != message)
			return message;
		message = matchWorkItemTypes(repo, pa, wiClient, wiCommon, monitor, p);
		if (null != message)
			return message;
		message = writeCategories(repo, pa, wiCommon, monitor, p);
		if (null != message)
			return message;
		message = writeDevelopmentLines(repo, pa, service, monitor, p);
		if (null != message)
			return message;
		message = writeWorkItems(repo, pa, wiClient, wiCommon, wiCopier, monitor, p);
		if (null != message)
			return message;

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
				return "user ID \"" + oldId
						+ "\" was in the source RTC project area (or has been at some point), but it has not been found in the user matching file";
			}
			member = members.get(newId);
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

	private static String matchWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p) {

		monitor.out("The workitem types are:");
		List<IWorkItemType> allWorkItemTypes;
		TaskType taskType;
		String type;
		try {
			allWorkItemTypes = wiClient.findWorkItemTypes(pa, monitor);
			for (IWorkItemType t : allWorkItemTypes) {
				type = t.getIdentifier();
				taskType = p.getTaskType(type);
				if (null == taskType) {
					return "can't find workitem type \"" + type + "\" in target project";
				}
				taskType.setExternalObject(t.getIdentifier(), t);
				monitor.out("\t" + t.getDisplayName() + " (" + type + ')');
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem while getting workitem types";
		}
		StateHelper.readWorkItemTypes(pa, wiClient, wiCommon, monitor);
		return null;
	}

	private static String writeCategories(ITeamRepository repo, IProjectArea pa, IWorkItemCommon wiCommon,
			ProgressMonitor monitor, Project p) {

		String message;
		for (Category cat : p.getCategories()) {
			message = CategoryHelper.createCategory(pa, wiCommon, monitor, p, cat);
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
			message = TimelineHelper.createLine(pa, service, monitor, p, line);
			if (null != message) {
				return "error creating line: " + message;
			}
			for (Iteration ite : line.getIterations()) {
				message = TimelineHelper.createIteration(pa, service, monitor, p, line, null, ite);
				if (null != message) {
					return "error creating iteration: " + message;
				}
			}
			message = TimelineHelper.setLineCurrent(pa, service, monitor, line);
			if (null != message) {
				return "error setting current iteration in line: " + message;
			}
		}
		return null;
	}

	private static String writeWorkItems(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p) {

		String message;
		for (Task t : p.getTasks()) {
			message = WorkItemHelper.createWorkItem(repo, pa, wiClient, wiCommon, wiCopier, monitor, p, t);
			if (null != message) {
				return "error creating workitem " + t.getId() + " (id in source): " + message;
			}
		}
		return null;
	}

}