package rtc.pa.read;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.ILink;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.IURIReference;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.IFetchResult;
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
import com.ibm.team.workitem.common.model.IApproval;
import com.ibm.team.workitem.common.model.IApprovalDescriptor;
import com.ibm.team.workitem.common.model.IApprovals;
import com.ibm.team.workitem.common.model.IAttachment;
import com.ibm.team.workitem.common.model.IAttachmentHandle;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IPriority;
import com.ibm.team.workitem.common.model.IResolution;
import com.ibm.team.workitem.common.model.ISeverity;
import com.ibm.team.workitem.common.model.ISubscriptions;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;

import rtc.pa.model.Approval;
import rtc.pa.model.Artifact;
import rtc.pa.model.Attachment;
import rtc.pa.model.Attribute;
import rtc.pa.model.Comment;
import rtc.pa.model.Link;
import rtc.pa.model.Literal;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskVersion;
import rtc.pa.model.Value;
import rtc.pa.utils.ProgressMonitor;

public class WorkItemHelper {

	static String readWorkItems(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor, Project p, String dir) {

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
						monitor, p, dir);
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
			IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor, Project p, String dir) {

		String result;
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
		monitor.out("\tjust added work item " + task.getSourceId() + "(" + task.getId() + ')');
		readWorkItemVersions(wi, repo, pa, wiClient, wiCommon, itemManager, monitor, p, task, dir);
		//
		// Links (includes attachments and artifacts (aka URIs)
		//
		result = readLinks(wi, repo, pa, wiClient, wiCommon, itemManager, monitor, p, task, dir);
		if (null != result)
			return result;
		//
		// Approvals
		//
		IApprovals approvals = wi.getApprovals();
		IApprovalDescriptor descriptor;
		for (IApproval approval : approvals.getContents()) {
			descriptor = approval.getDescriptor();
			task.addApproval(new Approval(//
					descriptor.getName(), //
					descriptor.getTypeIdentifier(), //
					descriptor.getDueDate(), //
					p.getMember(approval.getApprover().getItemId().getUuidValue())//
			));
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static String readWorkItemVersions(IWorkItem wi, ITeamRepository repo, IProjectArea pa,
			IWorkItemClient wiClient, IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor,
			Project p, Task task, String dir) {

		List<IWorkItem> workItems;
		try {
			workItems = itemManager.fetchCompleteStates(itemManager.fetchAllStateHandles(wi, monitor), monitor);
		} catch (TeamRepositoryException e1) {
			e1.printStackTrace();
			return "error reading history of work item " + wi.getId();
		}
		String result;
		for (IWorkItem w : workItems) {
			result = readWorkItemVersion(w, repo, pa, wiClient, wiCommon, itemManager, monitor, p, task, dir);
			if (null != result)
				return result;
		}
		return null;
	}

	private static String readWorkItemVersion(IWorkItem w, ITeamRepository repo, IProjectArea pa,
			IWorkItemClient wiClient, IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor,
			Project p, Task task, String dir) {

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
				resolution2//
		);
		//
		// Attributes
		//
		result = readAttributes(w, repo, pa, wiClient, wiCommon, itemManager, monitor, p, version);
		if (null != result)
			return result;
		//
		// Comments
		//
		for (IComment comment : w.getComments().getContents()) {
			version.addComment(new Comment(//
					w.getContextId().getUuidValue(), //
					p.getMember(comment.getCreator().getItemId().getUuidValue()), //
					comment.getCreationDate(), //
					(null == comment.getHTMLContent()) ? null : p.saver().get(comment.getHTMLContent().getXMLText())//
			));
		}
		//
		// Subscribers
		//
		ISubscriptions subscriptions = w.getSubscriptions();
		IContributorHandle[] subscribers = subscriptions.getContents();
		for (IContributorHandle subscriber : subscribers) {
			version.addSubscriber(p.getMember(subscriber.getItemId().getUuidValue()));
		}
		//
		// Save
		//
		p.putTaskVersion(version);
		monitor.out("\tjust added work item " + version.getSourceId() + "(" + version.getType() + " - "
				+ version.getTask().getId() + ')');

		return null;
	}

	private static String readAttributes(IWorkItem w, ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor, Project p,
			TaskVersion version) {

		IAttribute attribute;
		Attribute a;
		Literal l;
		List<IAttributeHandle> customAttributeHandles = w.getCustomAttributes();
		IFetchResult custom = null;
		try {
			custom = repo.itemManager().fetchCompleteItemsPermissionAware(customAttributeHandles, IItemManager.REFRESH,
					monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error finding custom attributes for work item version";
		}
		Object vObj;
		for (Object o : custom.getRetrievedItems()) {
			if (o instanceof IAttribute) {
				attribute = (IAttribute) o;
				a = p.getAttribute(attribute.getIdentifier());
				if (null == a) {
					return "error finding custom attribute " + attribute.getIdentifier();
				}
				if (a.isEnum()) {
					@SuppressWarnings("unchecked")
					Identifier<? extends ILiteral> id = (Identifier<? extends ILiteral>) w.getValue(attribute);
					l = a.getLiteral(id.getStringIdentifier());
					monitor.out("\t\tvalue (" + a.getSourceId() + ":" + a.getType() + "): " + id.getStringIdentifier());
					version.addValue(new Value(//
							attribute.getIdentifier(), //
							a, //
							l//
					));
				} else {
					monitor.out("\t\tvalue (" + a.getSourceId() + ":" + a.getType() + "): " + w.getValue(attribute));
					vObj = w.getValue((IAttribute) a.getExternalObject());
					if (vObj instanceof String) {
						version.addValue(new Value(//
								attribute.getIdentifier(), //
								a, //
								(String) vObj//
						));
					} else if (vObj instanceof Timestamp) {
						version.addValue(new Value(//
								attribute.getIdentifier(), //
								a, //
								(Timestamp) vObj//
						));
					} else if (vObj instanceof Boolean) {
						version.addValue(new Value(//
								attribute.getIdentifier(), //
								a, //
								(Boolean) vObj//
						));
					} else {
						monitor.out("\tvalue not taken into account: " + vObj);
					}
				}
			}
		}
		return null;
	}

	private static String readLinks(IWorkItem w, ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, ProgressMonitor monitor, Project p, Task task,
			String dir) {

		String result;
		IWorkItemReferences references;
		ILink link;
		try {
			references = wiCommon.resolveWorkItemReferences(w, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem resolving references for workitem " + task.getId();
		}
		IItemHandle referencedItem;
		IWorkItemHandle referencedWorkItem;
		IAttachmentHandle attachmentHandle;
		IAttachment attachment;
		IURIReference referencedURI;
		for (IEndPointDescriptor iEndPointDescriptor : references.getTypes()) {
			if (iEndPointDescriptor.isTarget()) {
				List<IReference> typedReferences = references.getReferences(iEndPointDescriptor);
				for (IReference ref : typedReferences) {
					link = ref.getLink();
					if (ref.isItemReference()) {
						referencedItem = ((IItemReference) ref).getReferencedItem();
						if (referencedItem instanceof IWorkItemHandle) {
							referencedWorkItem = (IWorkItemHandle) referencedItem;
							task.addLink(new Link(//
									link.getItemId().getUuidValue(), //
									referencedWorkItem.getItemId().getUuidValue(), //
									link.getLinkType().getLinkTypeId(), //
									ref.getComment()));
							monitor.out("\t\tjust added link (type: " + link.getLinkType().getLinkTypeId() + ") for "
									+ task.getSourceId() + " (" + task.getId() + ") to "
									+ referencedWorkItem.getItemId().getUuidValue());
						} else if (referencedItem instanceof IAttachmentHandle) {
							attachmentHandle = (IAttachmentHandle) ref.resolve();
							try {
								attachment = wiCommon.getAuditableCommon().resolveAuditable(attachmentHandle,
										IAttachment.DEFAULT_PROFILE, monitor);
							} catch (TeamRepositoryException e) {
								e.printStackTrace();
								return "can't resolve attachment handle";
							}
							result = AttachmentHelper.saveAttachment(attachment, dir, monitor);
							if (null != result) {
								return "error saving attachment: " + result;
							}
							task.addAttachment(new Attachment(//
									"" + attachment.getId(), //
									attachment.getName(), //
									attachment.getDescription(), //
									p.getMember(attachment.getCreator().getItemId().getUuidValue()), //
									attachment.getCreationDate() //
							));
							monitor.out("\t\tjust added attachment for " + task.getId());
						}
					} else if (ref.isURIReference()) {
						referencedURI = ((IURIReference) ref);
						if (!referencedURI.getURI().toString()
								.contains("http://www.ibm.com/support/knowledgecenter/")) {
							task.addArtifact(new Artifact(//
									link.getItemId().getUuidValue(), //
									referencedURI.getURI(), referencedURI.getComment()//
							));
							monitor.out("\t\tjust added artifact" + " for " + task.getSourceId() + " (" + task.getId()
									+ ") to " + referencedURI.getURI().toString() + "(" + referencedURI.getComment()
									+ ')');
						}
					}
				}
			}
		}
		return null;
	}

}
