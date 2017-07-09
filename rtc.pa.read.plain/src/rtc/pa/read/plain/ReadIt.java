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
import java.util.Date;
import java.util.List;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.ILink;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IDevelopmentLineHandle;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.IItemHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.Expression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.CategoryId;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.ICategoryHandle;
import com.ibm.team.workitem.common.model.IPriority;
import com.ibm.team.workitem.common.model.ISeverity;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;

import rtc.model.Administrator;
import rtc.model.Category;
import rtc.model.Iteration;
import rtc.model.Line;
import rtc.model.Link;
import rtc.model.Member;
import rtc.model.Project;
import rtc.model.Task;
import rtc.model.TaskVersion;
import rtc.utils.ProgressMonitor;

public class ReadIt {

	private static String trace(com.ibm.team.repository.common.IItemHandle item) {
		return "\n\n-> " + item + "\n";
	}

	private static String trace(String desc, com.ibm.team.repository.common.IItemHandle item) {
		return "\n\n" + desc + "-> " + item + "\n";
	}

	@SuppressWarnings("unused")
	private static String trace(String s) {
		return "\n\n-> \"" + s + "\"\n";
	}

	@SuppressWarnings("unused")
	private static String trace(String desc, String s) {
		return "\n\n" + desc + "-> \"" + s + "\"\n";
	}

	@SuppressWarnings("unused")
	private static String trace(Date d) {
		return "\n\n-> \"" + d + "\"\n";
	}

	@SuppressWarnings("unused")
	private static String trace(String desc, Date d) {
		return "\n\n" + desc + "-> \"" + d + "\"\n";
	}

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

		List<ICategory> allCategories;
		//
		// Categories
		//
		monitor.out("Now reading categories...");
		CategoryId parentId;
		try {
			allCategories = wiClient.findCategories(pa, ICategory.FULL_PROFILE, monitor);
			for (ICategory c : allCategories) {
				if (c.isArchived()) {
					continue;
				}
				parentId = c.getParentId2();
				p.putCategory(//
						new Category(//
								c.getCategoryId().getInternalRepresentation(), //
								c.getName(), //
								wiCommon.resolveHierarchicalName(c, monitor), //
								c.getHTMLDescription().getXMLText(), //
								((null == parentId) ? null : parentId.getInternalRepresentation())));
				monitor.out("\tjust added category " + c.getName() + trace(c));
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
		String message;
		Line line;
		IDevelopmentLineHandle[] devLines = pa.getDevelopmentLines();
		IDevelopmentLine devLine;
		IDevelopmentLineHandle current = pa.getProjectDevelopmentLine();
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
			if (devLine.isArchived()) {
				continue;
			}
			line = new Line(//
					devLine.getItemId().getUuidValue(), //
					devLine.getId(), //
					devLine.getName(), //
					devLine.getStartDate(), //
					devLine.getEndDate(), //
					devLine.getItemId().getUuidValue().equals(current.getItemId().getUuidValue()));
			p.putLine(line);
			monitor.out("\tjust added development line " + devLine.getName() + trace(devLine)
					+ trace("current", devLine.getCurrentIteration()));
			//
			// Iterations
			//
			for (IIterationHandle iterationHandle : devLine.getIterations()) {
				message = readIteration(devLine, devLine.getCurrentIteration(), iterationHandle, auditableClient,
						itemManager, "\t", monitor, p, line, null);
				if (null != message) {
					return "error reading iteration " + iterationHandle;
				}
			}
		}
		monitor.out("... development lines read.");
		return null;
	}

	private static String readIteration(IDevelopmentLine devLine, IIterationHandle currentIterationHandle,
			IIterationHandle iterationHandle, IAuditableClient auditableClient, IItemManager itemManager, String prefix,
			ProgressMonitor monitor, Project p, Line line, Iteration parent) {

		IIteration iteration;
		Iteration ite;
		try {
			iteration = auditableClient.resolveAuditable(iterationHandle, ItemProfile.ITERATION_DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error resolving iteration handle";
		}
		if (iteration.isArchived()) {
			return null;
		}
		ite = new Iteration(//
				iteration.getItemId().getUuidValue(), //
				iteration.getName(), //
				iteration.getId(), //
				iteration.getLabel(), //
				iteration.getDescription().getSummary(), //
				iteration.getStartDate(), //
				iteration.getEndDate());
		if (null == parent) {
			p.putIteration(line, ite);
		} else {
			p.putIteration(line, parent, ite);
		}
		if (iterationHandle.sameItemId(currentIterationHandle)) {
			monitor.out(prefix + "\tcurrent iteration!");
			line.setCurrent(ite);
		}
		monitor.out(prefix + "\tjust added iteration " + iteration.getName() + trace(iteration));
		for (IIterationHandle children : iteration.getChildren()) {
			readIteration(devLine, currentIterationHandle, children, auditableClient, itemManager, prefix + "\t",
					monitor, p, line, ite);
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
				monitor.out('\t' + t.getIdentifier());
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
		String result;
		try {
			while (results.hasNext(monitor)) {
				result = readWorkItem(results.next(monitor).getItem(), repo, pa, wiClient, wiCommon, itemManager,
						monitor, p);
				if (null != result)
					return result;
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error while reading set of work items";
		}
		monitor.out("... work items read.");
		return null;
	}

	private static String readWorkItem(IWorkItem wi, ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor, Project p) {

		Task task;
		//
		// Task
		//
		task = new Task(//
				wi.getItemId().getUuidValue(), //
				wi.getId(), //
				p.getMember(wi.getCreator().getItemId().getUuidValue()), //
				wi.getCreationDate());
		p.putTask(task);
		monitor.out("\tjust added work item " + task.getId() + trace(wi));
		readWorkItemVersions(wi, repo, pa, wiClient, wiCommon, itemManager, monitor, p, task);
		return null;
	}

	@SuppressWarnings("unchecked")
	private static String readWorkItemVersions(IWorkItem wi, ITeamRepository repo, IProjectArea pa,
			IWorkItemClient wiClient, IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor,
			Project p, Task task) {

		List<IWorkItem> workItems;
		try {
			workItems = itemManager.fetchCompleteStates(itemManager.fetchAllStateHandles(wi, monitor), monitor);
		} catch (TeamRepositoryException e1) {
			e1.printStackTrace();
			return "error reading history of work item " + wi.getId();
		}
		String result;
		for (IWorkItem w : workItems) {
			result = readWorkItemVersion(w, repo, pa, wiClient, wiCommon, itemManager, monitor, p, task);
			if (null != result)
				return result;
		}
		return null;
	}

	private static String readWorkItemVersion(IWorkItem w, ITeamRepository repo, IProjectArea pa,
			IWorkItemClient wiClient, IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor,
			Project p, Task task) {

		XMLString description = w.getHTMLDescription();
		XMLString summary = w.getHTMLSummary();
		Identifier<IPriority> priority = w.getPriority();
		Identifier<ISeverity> severity = w.getSeverity();
		List<String> tags2 = w.getTags2();
		List<String> tags = new ArrayList<String>();
		for (String t : tags2) {
			tags.add(t);
		}
		ICategoryHandle category = w.getCategory();
		IIterationHandle target = w.getTarget();
		IContributor ownedBy;
		try {
			ownedBy = (IContributor) repo.itemManager().fetchCompleteItem((IContributorHandle) w.getOwner(),
					IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem retrieving owner for workitem " + task.getId();
		}
		IContributor resolvedBy;
		try {
			resolvedBy = (IContributor) repo.itemManager().fetchCompleteItem((IContributorHandle) w.getResolver(),
					IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem retrieving resolver for workitem " + task.getId();
		}
		// TO DO
		// w.getApprovals();
		// w.getComments();
		// w.getCustomAttributes();
		// w.getState2();
		// w.getValue(null);

		//
		// TaskVersion
		//
		TaskVersion version;
		version = new TaskVersion(//
				w.getItemId().getUuidValue(), //
				w.getWorkItemType(), //
				p.getMember(w.getModifiedBy().getItemId().getUuidValue()), //
				w.modified(), //
				((null == summary) ? null : summary.getXMLText()), //
				((null == description) ? null : description.getXMLText()), //
				((null == priority) ? null : priority.getStringIdentifier()), //
				((null == severity) ? null : severity.getStringIdentifier()), //
				tags, //
				w.getDueDate(), //
				w.getDuration(), //
				((null == category) ? null : p.getCategory(category.getItemId().getUuidValue())), //
				((null == target) ? null : p.getIteration(target.getItemId().getUuidValue())), //
				p.getMember(ownedBy.getItemId().getUuidValue()), //
				p.getMember(resolvedBy.getItemId().getUuidValue()), //
				w.getResolutionDate());
		task.putTaskVersion(version);
		monitor.out("\tjust added work item version " + task.getId() + trace(w));
		//
		// Links
		//
		IWorkItemReferences references;
		ILink link;
		try {
			references = wiCommon.resolveWorkItemReferences(w, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem resolving references for workitem " + task.getId();
		}
		IItemHandle referencedItem;
		for (IEndPointDescriptor iEndPointDescriptor : references.getTypes()) {
			monitor.out("END POINT (" + task.getId() + "): " + iEndPointDescriptor.getDisplayName() + " ID: "
					+ iEndPointDescriptor.getLinkType().getLinkTypeId() + " - " + iEndPointDescriptor);
			List<IReference> typedReferences = references.getReferences(iEndPointDescriptor);
			for (IReference ref : typedReferences) {
				if (ref.isItemReference()) {
					referencedItem = ((IItemReference) ref).getReferencedItem();
					if (referencedItem instanceof IWorkItemHandle) {
						link = ref.getLink();
						version.addLink(new Link(//
								w.getItemId().getUuidValue(), //
								referencedItem.getItemId().getUuidValue(), //
								link.getLinkType().getLinkTypeId()));
						monitor.out("\t\tjust added link for " + task.getId());
					}
				}
			}
		}
		return null;
	}

}
