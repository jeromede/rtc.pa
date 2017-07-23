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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IApproval;
import com.ibm.team.workitem.common.model.IApprovalDescriptor;
import com.ibm.team.workitem.common.model.IApprovals;
import com.ibm.team.workitem.common.model.IAttachment;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.IComment;
import com.ibm.team.workitem.common.model.IComments;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IPriority;
import com.ibm.team.workitem.common.model.IResolution;
import com.ibm.team.workitem.common.model.ISeverity;
import com.ibm.team.workitem.common.model.ISubscriptions;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;

import rtc.pa.model.Approval;
import rtc.pa.model.Artifact;
import rtc.pa.model.Attachment;
import rtc.pa.model.Category;
import rtc.pa.model.Comment;
import rtc.pa.model.Iteration;
import rtc.pa.model.Link;
import rtc.pa.model.Literal;
import rtc.pa.model.Member;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskVersion;
import rtc.pa.model.Value;
import rtc.pa.utils.ProgressMonitor;

public class WorkItemCopyBuilder {

	@SuppressWarnings("serial")
	private static final Map<String, IEndPointDescriptor> linkTypes = Collections
			.unmodifiableMap(new HashMap<String, IEndPointDescriptor>() {
				{
					put(WorkItemLinkTypes.BLOCKS_WORK_ITEM, WorkItemEndPoints.BLOCKS_WORK_ITEM);
					put(WorkItemLinkTypes.COPIED_WORK_ITEM, WorkItemEndPoints.COPIED_WORK_ITEM);
					put(WorkItemLinkTypes.DUPLICATE_WORK_ITEM, WorkItemEndPoints.DUPLICATE_WORK_ITEM);
					put(WorkItemLinkTypes.PARENT_WORK_ITEM, WorkItemEndPoints.PARENT_WORK_ITEM);
					put(WorkItemLinkTypes.RELATED_WORK_ITEM, WorkItemEndPoints.RESOLVES_WORK_ITEM);
					put(WorkItemLinkTypes.MENTIONS, WorkItemEndPoints.MENTIONS);
				}
			});

	static String fillMinimalWorkItemVersion(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p, IWorkItem wi, TaskVersion version) {

		monitor.out(
				"Fill (first) new work item version with minimal information for (summary): " + version.getSummary());
		String result;
		result = fillMinimal(repo, pa, wiClient, wiCommon, monitor, wi, version);
		if (null != result)
			return result;
		return null;
	}

	private static String fillMinimal(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, IWorkItem wi, TaskVersion version) {

		//
		// migration specifics
		//
		IAttribute modifierInSource = null;
		IAttribute modifiedInSource = null;
		IAttribute sourceId = null;
		try {
			modifierInSource = wiClient.findAttribute(pa, "rtc.pa.modifier", monitor);
			modifiedInSource = wiClient.findAttribute(pa, "rtc.pa.modified", monitor);
			sourceId = wiClient.findAttribute(pa, "rtc.pa.id", monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			monitor.err(
					"Can't find special custom attributes <rtc.pa.modifier> and/nor <rtc.pa.modified> and/nor <rtc.pa.id>"
							+ " that could exist in target to reflect the old work item history as read from source."
							+ " Continue anyway.");
		}
		if (null != modifiedInSource) {
			wi.setValue(modifiedInSource, new Timestamp(version.getModified().getTime()));
		}
		if (null != modifierInSource) {
			wi.setValue(modifierInSource, WorkItemBuilder.getC(repo, version.getModifier()));
		}
		if (null != sourceId) {
			wi.setValue(sourceId, new Integer(version.getTask().getId()));
		}
		//
		// summary
		//
		if (null == version.getSummary()) {
			wi.setHTMLSummary(null);
		} else {
			wi.setHTMLSummary(XMLString.createFromXMLText(version.getSummary()));
		}
		//
		// category
		//
		ICategory category;
		Category cat = version.getCategory();
		if (null == cat) {
			try {
				category = wiCommon.findUnassignedCategory(pa, ICategory.FULL_PROFILE, monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "can't find /unassigned/ category";
			}
		} else {
			category = (ICategory) cat.getExternalObject();
		}
		wi.setCategory(category);

		return null;
	}

	static String fillWorkItemVersion(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Map<Integer, Task> tasks, Project p, IWorkItem wi,
			TaskVersion version, Map<Timestamp, Comment> currentComments) {

		monitor.out("Fill new work item version for (summary): " + version.getSummary());
		String result;
		result = fillMinimal(repo, pa, wiClient, wiCommon, monitor, wi, version);
		if (null != result)
			return result;
		//
		// description
		//
		if (null == version.getDescription()) {
			wi.setHTMLDescription(null);
		} else {
			wi.setHTMLDescription(XMLString.createFromXMLText(transpose(version.getDescription(), tasks)));
		}
		//
		// priority
		//
		Identifier<IPriority> priorityId = Identifier.create(IPriority.class, version.getPriority());
		wi.setPriority(priorityId);
		//
		// severity
		//
		Identifier<ISeverity> severityId = Identifier.create(ISeverity.class, version.getSeverity());
		wi.setSeverity(severityId);
		//
		// tags
		//
		List<String> tags = new ArrayList<String>(version.getTags().size());
		tags.addAll(version.getTags());
		wi.setTags2(tags);
		//
		// due date
		//
		wi.setDueDate(version.getDue());
		//
		// duration
		//
		wi.setDuration(version.getDuration());
		//
		// target
		//
		IIteration iteration;
		Iteration target = version.getTarget();
		if (null == target) {
			iteration = null;
		} else {
			iteration = (IIteration) version.getTarget().getExternalObject();
		}
		wi.setTarget(iteration);
		//
		// owner
		//
		wi.setOwner(WorkItemBuilder.getC(repo, version.getOwnedBy()));
		//
		// resolution
		//
		Identifier<IResolution> resolution2Id;
		if (null == version.getResolution2()) {
			resolution2Id = null;
		} else {
			resolution2Id = Identifier.create(IResolution.class, version.getResolution2());
		}
		wi.setResolution2(resolution2Id);
		//
		// values (custom attributes)
		//
		for (Value val : version.getValues()) {
			IAttribute attribute = (IAttribute) val.getAttribute().getExternalObject();
			monitor.out('\t' + attribute.getIdentifier() + " : " + attribute.getAttributeType());
			if (wi.hasAttribute(attribute)) {
				if (val.getAttribute().isEnum()) {
					if (null == val.getValue()) {
						wi.setValue(attribute, null);
					} else {
						@SuppressWarnings("unchecked")
						Identifier<? extends ILiteral> literalId = //
								(Identifier<? extends ILiteral>) //
								val.getAttribute()//
										.getLiteral(//
												((Literal) val.getValue())//
														.getSourceId()//
										)//
										.getExternalObject();
						wi.setValue(attribute, literalId);
					}
				} else {
					wi.setValue(attribute, val.getValue());
				}
				monitor.out("\t\t= " + val.getValue());
			} else {
				monitor.out("\t\t... no!");
			}
		}
		//
		// comments
		//
		IComments comments = wi.getComments();
		IComment comment;
		XMLString signature;
		for (Comment comm : version.getComments()) {
			if (!currentComments.containsKey(comm.getCreation())) {
				signature = XMLString
						.createFromXMLText("<p>&nbsp;</p><p>&nbsp;</p><p><em>Original " + comm.getCreation() + " ("
								+ WorkItemBuilder.getC(repo, comm.getCreator()).getName() + ")</em></p>");
				comment = comments.createComment(//
						WorkItemBuilder.getC(repo, comm.getCreator()), //
						XMLString.createFromXMLText(transpose(comm.getContent(), tasks)).concat(signature)//
				);
				comments.append(comment);
				currentComments.put(comm.getCreation(), comm);
			}
		}
		//
		// subscribers
		//
		ISubscriptions subscriptions = wi.getSubscriptions();
		Map<String, IContributorHandle> real = new HashMap<String, IContributorHandle>();
		Collection<IContributorHandle> toBeRemoved = new ArrayList<IContributorHandle>();
		IContributorHandle subscriber;
		for (Member subscr : version.getSubscribers()) {
			subscriber = (IContributorHandle) WorkItemBuilder.getC(repo, subscr).getItemHandle();
			real.put(subscriber.getItemId().getUuidValue(), subscriber);
		}
		for (IContributorHandle contributor : subscriptions.getContents()) {
			if (!real.containsKey(contributor.getItemId().getUuidValue())) {
				toBeRemoved.add(contributor);
			}
		}
		for (IContributorHandle contributor : toBeRemoved) {
			subscriptions.remove(contributor);
		}
		subscriptions = wi.getSubscriptions();
		for (Member subscr : version.getSubscribers()) {
			subscriber = (IContributorHandle) WorkItemBuilder.getC(repo, subscr).getItemHandle();
			if (!subscriptions.contains(subscriber)) {
				subscriptions.add(subscriber);
			}
		}

		return null;
	}

	private static String transpose(String content, Map<Integer, Task> tasks) {
		// TODO
		return content;
	}

	static String updateWorkItemCopyWithLinks(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, String dir,
			IWorkItem wi, IWorkItemHandle wiH, Project p, Task task) {

		String result;
		WorkItemWorkingCopy wc = wiCopier.getWorkingCopy(wiH);

		//
		// links
		//
		result = WorkItemCopyBuilder.createLinks(wi, wc, task, monitor);
		if (null != result)
			return result;
		//
		// artifacts
		//
		result = WorkItemCopyBuilder.createArtifacts(wi, wc, task, monitor);
		if (null != result)
			return result;
		//
		// attachments
		//
		result = WorkItemCopyBuilder.createAttachments(pa, wiClient, wiCommon, wi, wc, task, monitor, dir);
		if (null != result)
			return result;
		//
		// approvals
		//
		result = WorkItemCopyBuilder.createApprovals(wc, task);
		if (null != result)
			return result;

		IDetailedStatus s = wc.save(monitor);
		if (!s.isOK()) {
			s.getException().printStackTrace();
			return ("error updating work item " + wc.getWorkItem().getId());
		}
		return null;
	}

	private static String createLinks(IWorkItem wi, WorkItemWorkingCopy wc, Task task, ProgressMonitor monitor) {

		IWorkItem otherWi;
		IItemReference reference;
		IEndPointDescriptor endpoint;
		for (Link l : task.getLinks()) {
			endpoint = linkTypes.get(l.getType());
			if (null == endpoint)
				continue;
			if (null == l.getTarget())
				continue;
			otherWi = (IWorkItem) l.getTarget().getExternalObject();
			if (null == otherWi)
				continue;
			reference = IReferenceFactory.INSTANCE.createReferenceToItem(otherWi.getItemHandle());
			monitor.out(
					"\tabout to create link " + l.getType() + " from " + wi.getId() + " to " + otherWi.getId() + "...");
			wc.getReferences().add(endpoint, reference);
			monitor.out("\t... link created");
		}
		return null;
	}

	private static String createArtifacts(IWorkItem wi, WorkItemWorkingCopy wc, Task task, ProgressMonitor monitor) {

		IReference reference;
		IEndPointDescriptor endpoint;
		endpoint = WorkItemEndPoints.RELATED_ARTIFACT;
		for (Artifact a : task.getArtifacts()) {
			reference = IReferenceFactory.INSTANCE.createReferenceFromURI(a.getURI(), a.getComment());
			monitor.out("\tabout to create artifact " + a.getURI().getPath() + " from " + wi.getId() + "...");
			wc.getReferences().add(endpoint, reference);
			monitor.out("\t... artifact created");
		}
		return null;
	}

	private static String createAttachments(IProjectArea pa, IWorkItemClient wiClient, IWorkItemCommon wiCommon,
			IWorkItem wi, WorkItemWorkingCopy wc, Task task, ProgressMonitor monitor, String dir) {

		String filename;
		File inputFile;
		FileInputStream in;
		IItemReference reference;
		IAttachment attachment;
		for (Attachment att : task.getAttachments()) {
			filename = dir + File.separator + att.getSourceId() + '.' + att.getName();
			inputFile = new File(filename);
			monitor.out("\tabout to upload attachement from file " + filename);
			try {
				in = new FileInputStream(inputFile);
				try {
					attachment = wiClient.createAttachment(//
							pa, //
							att.getName(), //
							att.getDescription() + " attached by " + att.getCreator().getName() + " "
									+ att.getCreation(), //
							att.getContentType(), //
							att.getEncoding(), //
							in, //
							monitor);
					attachment = (IAttachment) attachment.getWorkingCopy();
					attachment = wiCommon.saveAttachment(attachment, monitor);
					reference = WorkItemLinkTypes.createAttachmentReference(attachment);
					wc.getReferences().add(WorkItemEndPoints.ATTACHMENT, reference);
				} catch (TeamRepositoryException e) {
					e.printStackTrace();
					return "problem while uploading attachment for file " + filename;
				} finally {
					if (null != in)
						in.close();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return "file to attach not found " + filename;
			} catch (IOException e) {
				e.printStackTrace();
				return "i/o error while uploading file " + filename;
			}
			monitor.out("\t... done.");
		}
		return null;
	}

	private static String createApprovals(WorkItemWorkingCopy wc, Task task) {

		class AproovalDesc {
			String type;
			String name;
			Timestamp due;

			public int hashCode() {
				return 0;
			}

			public boolean equals(Object o) {
				if (!(o instanceof AproovalDesc))
					return false;
				AproovalDesc other = (AproovalDesc) o;
				return this.type.equals(other.type) && this.name.equals(other.name) && this.due.equals(other.due);
			}
		}

		class Aproover {
			String state;
			String ap;

			public int hashCode() {
				return 0;
			}

			public boolean equals(Object o) {
				if (!(o instanceof Aproover))
					return false;
				Aproover other = (Aproover) o;
				return this.state.equals(other.state) && this.ap.equals(other.ap);
			}
		}

		class Aprooval {
			Map<Aproover, IContributor> cont = new HashMap<Aproover, IContributor>();
			Map<String, Aproover> aps = new HashMap<String, Aproover>();
		}

		IApprovals approvals = wc.getWorkItem().getApprovals();
		IApprovalDescriptor descriptor;
		IApproval approval;
		IContributor approver;
		Map<AproovalDesc, Aprooval> aproovals = new HashMap<AproovalDesc, Aprooval>();
		AproovalDesc approvalDesc;
		Aprooval aprooval;
		Aproover aproover;
		for (Approval a : task.getApprovals()) {
			approvalDesc = new AproovalDesc();
			approvalDesc.type = a.getType();
			approvalDesc.name = a.getSourceId();
			approvalDesc.due = a.getDue();
			System.out.println("aproovals before: " + aproovals);
			aprooval = aproovals.get(approvalDesc);
			if (null == aprooval) {
				aprooval = new Aprooval();
				aproovals.put(approvalDesc, aprooval);
				System.out.println("aproovals after: " + aproovals);
			}
			approver = null;
			if (null != a.getApprover()) {
				approver = (IContributor) a.getApprover().getExternalObject();
			}
			if (null != approver) {
				aproover = aprooval.aps.get(approver.getUserId());
				if (null == aproover) {
					aproover = new Aproover();
					aproover.state = (null == a.getState()) ? "" : a.getState();
					aproover.ap = approver.getUserId();
				}
				aprooval.cont.put(aproover, approver);
				aprooval.aps.put(aproover.ap, aproover);
			}
		}

		for (AproovalDesc desc : aproovals.keySet()) {
			aprooval = aproovals.get(desc);
			System.out.println(desc.type + '/' + desc.name + '/' + desc.due);
			for (Aproover apr : aprooval.aps.values()) {
				approver = aprooval.cont.get(apr);
				System.out.println("\t" + approver.getName() + '/' + apr.state);
			}
		}

		for (AproovalDesc desc : aproovals.keySet()) {
			aprooval = aproovals.get(desc);
			approval = null;
			descriptor = approvals.createDescriptor(desc.type, desc.name);
			descriptor.setDueDate(desc.due);
			for (Aproover apr : aprooval.aps.values()) {
				approver = aprooval.cont.get(apr);
				approval = approvals.createApproval(descriptor, approver);
				if (0 != apr.state.length()) {
					approval.setStateIdentifier(apr.state);
				}
				approvals.add(approval);
			}
		}
		return null;
	}

}
