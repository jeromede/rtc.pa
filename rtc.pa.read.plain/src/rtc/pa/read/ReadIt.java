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

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;

import rtc.pa.model.Link;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.utils.ProgressMonitor;

public class ReadIt {

	public static String execute(ITeamRepository repo, IProjectArea pa, boolean complete, ProgressMonitor monitor, Project p, String dir)
			throws TeamRepositoryException, IOException {

		IItemManager itemManager = repo.itemManager();
		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);
		String result;

		result = UserHelper.readMembers(repo, pa, monitor, p);
		if (null != result)
			return result;
		result = WorkItemTypeHelper.readWorkItemTypes(repo, pa, wiClient, wiCommon, monitor, p);
		if (null != result)
			return result;
		result = CategoryHelper.readCategories(repo, pa, wiClient, wiCommon, monitor, p);
		if (null != result)
			return result;
		result = TimelineHelper.readDevelopmentLines(repo, pa, auditableClient, itemManager, monitor, p);
		if (null != result)
			return result;
		result = WorkItemHelper.readWorkItems(repo, pa, complete, wiClient, wiCommon, itemManager, monitor, p, dir);
		if (null != result)
			return result;
		resolve(p);
		return null;
	}

	private static void resolve(Project p) {
		for (Task task : p.getTasks()) {
			for (Link link : task.getLinks()) {
				System.out.println("resolve link (type: " //
						+ link.getType() //
						+ ") " //
						+ link.getTargetId() //
						+ "\n\tfrom " + task.getId() //
				);
				link.resolve(p.getTask(link.getTargetId()));
				if (null == link.getTarget()) {
					System.out.println("\tto   unknown target work item (" + link.getTargetId() + ')');
				} else {
					System.out.println("\tto   " + link.getTarget().getId());
				}
			}
		}
	}

}
