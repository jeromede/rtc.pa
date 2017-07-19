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

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.common.IWorkItemCommon;

import rtc.pa.model.Link;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.utils.ProgressMonitor;

public class LinkHelper {

	public static String createLinks(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			Task t) {

		//
		// TODO: links
		//
		for (Link l : t.getLinks()) {
			l.getTarget();
		}

		return null;
	}

/* *********
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

********* */

}
