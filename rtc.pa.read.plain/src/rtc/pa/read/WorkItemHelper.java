package rtc.pa.read;

import java.util.ArrayList;
import java.util.List;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.ILink;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
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
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.IPriority;
import com.ibm.team.workitem.common.model.IResolution;
import com.ibm.team.workitem.common.model.ISeverity;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;

import rtc.pa.model.Attribute;
import rtc.pa.model.Link;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskVersion;
import rtc.pa.utils.ProgressMonitor;

public class WorkItemHelper {

	static String readWorkItems(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
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

	static String readWorkItem(IWorkItem wi, ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
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
		monitor.out("\tjust added work item " + task.getId() + '\n' + wi);
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

		String result;
		XMLString summary = w.getHTMLSummary();
		XMLString description = w.getHTMLDescription();
		Identifier<IPriority> priority = w.getPriority();
		Identifier<ISeverity> severity = w.getSeverity();
		List<String> tags2 = w.getTags2();
		List<String> tags = new ArrayList<String>();
		for (String t : tags2) {
			tags.add(t);
		}
		ICategory category;
		try {
			category = (ICategory) itemManager.fetchCompleteItem(w.getCategory(), IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "can't fetch category from handle";
		}
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
		Identifier<IResolution> resolution2Id = w.getResolution2();
		String resolution2 = null;
		if (null != resolution2Id) {
			resolution2 = resolution2Id.getStringIdentifier();
		}
		// TODO: use generic attributes instead (for builtin and custom) --
		// w.getValue(IAttribute)
		// w.getApprovals();
		// w.getComments();
		// w.getCustomAttributes();
		//
		// TaskVersion
		//
		TaskVersion version = new TaskVersion(//
				w.getItemId().getUuidValue(), //
				task, //
				p.getTaskType(w.getWorkItemType()), //
				w.getState2().getStringIdentifier(), //
				p.getMember(w.getModifiedBy().getItemId().getUuidValue()), //
				w.modified(), //
				((null == summary) ? null : p.saver().get(summary.getXMLText())), //
				((null == description) ? null : p.saver().get(description.getXMLText())), //
				((null == priority) ? null : p.saver().get(priority.getStringIdentifier())), //
				((null == severity) ? null : p.saver().get(severity.getStringIdentifier())), //
				tags, //
				w.getDueDate(), //
				w.getDuration(), //
				((null == category) ? null : p.getCategory(category.getCategoryId().getInternalRepresentation())), //
				((null == target) ? null : p.getIteration(target.getItemId().getUuidValue())), //
				UserHelper.getM(p, ownedBy), //
				UserHelper.getM(p, resolvedBy), //
				w.getResolutionDate(), //
				resolution2);
		//
		// Links
		//
		result = readLinks(w, repo, pa, wiClient, wiCommon, itemManager, monitor, p, version);
		if (null != result)
			return result;
		//
		// Attributes
		//
		result = readAttributes(w, repo, pa, wiClient, wiCommon, itemManager, monitor, p, version);
		if (null != result)
			return result;
		//
		// Save
		//
		p.putTaskVersion(version);
		monitor.out("\tjust added work item version " + task.getId() + '\n' + w);

		return null;
	}

	private static String readAttributes(IWorkItem w, ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor, Project p,
			TaskVersion version) {

		IAttribute attribute = null;
		w.getValue(attribute);
		for (Attribute a : version.getType().getAttributes()) {
			w.getValue((IAttribute) a.getExternalObject());
		}
		return null;
	}

	private static String readLinks(IWorkItem w, ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor, Project p,
			TaskVersion version) {
		IWorkItemReferences references;
		ILink link;
		try {
			references = wiCommon.resolveWorkItemReferences(w, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem resolving references for workitem " + version.getTask().getId();
		}
		IItemHandle referencedItem;
		for (IEndPointDescriptor iEndPointDescriptor : references.getTypes()) {
			monitor.out("END POINT (" + version.getTask().getId() + "): " + iEndPointDescriptor.getDisplayName()
					+ " ID: " + iEndPointDescriptor.getLinkType().getLinkTypeId() + " - " + iEndPointDescriptor);
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
						monitor.out("\t\tjust added link for " + version.getTask().getId());
					}
				}
			}
		}
		return null;
	}

}