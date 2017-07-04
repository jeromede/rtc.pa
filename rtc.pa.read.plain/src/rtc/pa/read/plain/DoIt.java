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

package rtc.pa.read.plain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IDevelopmentLineHandle;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IContributorManager;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.scm.client.IWorkspaceManager;
import com.ibm.team.scm.client.SCMPlatform;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.Expression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.ICategoryHandle;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;

import rtc.model.Administrator;
import rtc.model.Category;
import rtc.model.Iteration;
import rtc.model.Line;
import rtc.model.Member;
import rtc.model.Project;
import rtc.model.Task;
import rtc.model.TaskVersion;
import rtc.utils.ProgressMonitor;

public class DoIt {

	public static String execute(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p)
			throws TeamRepositoryException, IOException {

		IItemManager itemManager = repo.itemManager();
		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);
		String result;

		result = readMembers(repo, pa, monitor, p);
		if (null != result)
			return result;
		result = readCategories(repo, pa, wiClient, wiCommon, monitor, p);
		if (null != result)
			return result;
		result = readDevelopmentLines(repo, pa, auditableClient, itemManager, monitor, p);
		if (null != result)
			return result;
		result = readWorkItemTypes(repo, pa, wiClient, wiCommon, monitor, p);
		if (null != result)
			return result;
		result = readWorkItems(repo, pa, wiClient, wiCommon, itemManager, monitor, p);
		if (null != result)
			return result;

		return null;
	}

	private static String readContributors(IContributorHandle[] contributors, ITeamRepository repo, IProjectArea pa,
			ProgressMonitor monitor, Project p, boolean isAdmin) {

		IContributor contrib;
		for (IContributorHandle contribHandle : contributors) {
			try {
				contrib = (IContributor) repo.itemManager().fetchCompleteItem(contribHandle, IItemManager.DEFAULT,
						monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error resolving IContributorHandle";
			}
			if (isAdmin) {
				p.putAdministrator(
						new Administrator(contrib.getItemId().getUuidValue(), contrib.getUserId(), contrib.getName()));
				monitor.out("\tjust added administrator " + contrib.getUserId());
			} else {
				p.putMember(new Member(contrib.getItemId().getUuidValue(), contrib.getUserId(), contrib.getName()));
				monitor.out("\tjust added member " + contrib.getUserId());
			}
		}
		return null;
	}

	private static String readMembers(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p) {

		String result;
		monitor.out("Now reading administrators...");
		result = readContributors(pa.getAdministrators(), repo, pa, monitor, p, true);
		monitor.out("... administrators read.");
		if (null != result)
			return result;
		monitor.out("Now reading members...");
		result = readContributors(pa.getMembers(), repo, pa, monitor, p, false);
		monitor.out("... members read.");
		if (null != result)
			return result;
		return null;
	}

	private static String readCategories(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p) {

		com.ibm.team.foundation.common.text.XMLString x;
		List<ICategory> allCategories;
		//
		// Categories
		//
		monitor.out("Now reading categories...");
		try {
			allCategories = wiClient.findCategories(pa, ICategory.FULL_PROFILE, monitor);
			for (ICategory c : allCategories) {
				p.putCategory(new Category(c.getItemId().getUuidValue(), c.getName(),
						wiCommon.resolveHierarchicalName(c, monitor), c.getHTMLDescription().getXMLText()));
				monitor.out("\tjust added category " + c.getName());
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error reading categories";
		}
		monitor.out("... categories read.");
		return null;
	}

	private static String readDevelopmentLines(ITeamRepository repo, IProjectArea pa, IAuditableClient auditableClient,
			IItemManager itemManager, ProgressMonitor monitor, Project p) {

		monitor.out("Now reading development lines...");
		Line line;
		IDevelopmentLineHandle[] devLines = pa.getDevelopmentLines();
		IDevelopmentLine devLine;
		for (IDevelopmentLineHandle devLineHandle : devLines) {
			//
			// Development line
			//
			try {
				devLine = auditableClient.resolveAuditable(devLineHandle, ItemProfile.DEVELOPMENT_LINE_DEFAULT,
						monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error resolving development line handle";
			}
			line = new Line(devLine.getName());
			p.putLine(line);
			monitor.out("\tjust added development line " + devLine.getName());
			//
			// Iterations
			//
			for (IIterationHandle iterationHandle : devLine.getIterations()) {
				readIteration(devLine, iterationHandle, repo, pa, auditableClient, itemManager, "\t", monitor, p, line);
			}
		}
		monitor.out("... development lines read.");
		return null;
	}

	private static String readIteration(IDevelopmentLine devLine, IIterationHandle iterationHandle,
			ITeamRepository repo, IProjectArea pa, IAuditableClient auditableClient, IItemManager itemManager,
			String prefix, ProgressMonitor monitor, Project p, Line line) {

		IIteration iteration;
		try {
			iteration = auditableClient.resolveAuditable(iterationHandle, ItemProfile.ITERATION_DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error resolving iteration handle";
		}
		line.putIteration(new Iteration(iteration.getName()));
		monitor.out(prefix + "\tjust added iteration " + iteration.getName());
		for (IIterationHandle children : iteration.getChildren()) {
			readIteration(devLine, children, repo, pa, auditableClient, itemManager, prefix + "\t", monitor, p, line);
		}
		return null;
	}

	private static String readWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p) {

		monitor.out("Now reading work item types...");
		List<IWorkItemType> allWorkItemTypes;
		try {
			allWorkItemTypes = wiClient.findWorkItemTypes(pa, monitor);
			for (IWorkItemType t : allWorkItemTypes) {
				monitor.out(t.getIdentifier());
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error reading work item types";
		}
		monitor.out("... work item types read.");
		return null;
	}

	private static String readWorkItems(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor, Project p) {

		monitor.out("Now reading work items...");
		Task task;
		IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);
		IQueryClient queryClient = (IQueryClient) repo.getClientLibrary(IQueryClient.class);
		IQueryableAttribute attribute = null;
		try {
			attribute = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE).findAttribute(pa,
					IWorkItem.PROJECT_AREA_PROPERTY, auditableClient, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error finding work item attributes";
		}
		Expression expression = new AttributeExpression(attribute, AttributeOperation.EQUALS, pa);
		IQueryResult<IResolvedResult<IWorkItem>> results = queryClient.getResolvedExpressionResults(pa, expression,
				IWorkItem.FULL_PROFILE);
		results.setLimit(Integer.MAX_VALUE);
		IResolvedResult<IWorkItem> result;
		IWorkItem wi;
		try {
			while (results.hasNext(monitor)) {
				result = results.next(monitor);
				wi = result.getItem();
				task = new Task(wi.getItemId().getUuidValue(), p.getMember(wi.getCreator().getItemId().getUuidValue()));
				p.putTask(task);
				readWorkItem(wi, repo, pa, wiClient, wiCommon, itemManager, "", monitor, p, task);
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error while reading set of work items";
		}
		monitor.out("... work items read.");
		return null;
	}

	private static String readWorkItem(IWorkItem wi, ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, String prefix, ProgressMonitor monitor,
			Project p, Task task) {

		IAuditableHandle auditableHandle = (IAuditableHandle) wi.getItemHandle();
		List<IWorkItemHandle> handles;
		List<IWorkItem> workItems;
		try {
			handles = itemManager.fetchAllStateHandles(auditableHandle, monitor);
			workItems = itemManager.fetchCompleteStates(handles, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Error reading history of work item " + wi.getId();
		}
		for (IWorkItem w : workItems) {
//			readWorkItemVersion(w, repo, pa, wiClient, wiCommon, itemManager, prefix + "\t", monitor, p, task);
		}

		return null;
	}

	private static String readWorkItemVersion(IWorkItem wi, ITeamRepository repo, IProjectArea pa,
			IWorkItemClient wiClient, IWorkItemCommon wiCommon, IItemManager itemManager, String prefix,
			ProgressMonitor monitor, Project p, Task task) {

		monitor.out("\n" + prefix + "workitem " + wi.getId() + " " + wi.getCreationDate() + " — " + wi.getFullState()
				.toString().replaceAll(", ", "\n " + prefix).replaceAll(" \\(", "\n" + prefix + "\\("));
		monitor.out(prefix + "TARGET: " + ((null == wi.getTarget()) ? "null" : wi.getTarget().toString()));
		monitor.out(prefix + "MODIFIED: " + wi.modified().toString());
		IContributor contributor;
		try {
			contributor = (IContributor) repo.itemManager().fetchCompleteItem((IContributorHandle) wi.getModifiedBy(),
					IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Problem retrieving contributor for workitem.";
		}
		monitor.out(prefix + "MODIFIER: " + contributor.getUserId() + " (" + contributor.getName() + " <"
				+ contributor.getEmailAddress() + ">)");
		try {
			monitor.out(prefix + "CATEGORY: "
					+ wiCommon.resolveHierarchicalName((ICategoryHandle) wi.getCategory(), monitor));
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Error while reading category for work item " + wi.getId();
		}
		monitor.out(prefix + "TARGET: " + ((null == wi.getTarget()) ? "null" : wi.getTarget().toString()));

		return null;
	}

}
