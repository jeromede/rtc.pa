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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.team.links.common.IItemReference;
import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.factory.IReferenceFactory;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttachment;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;

import rtc.pa.model.Approval;
import rtc.pa.model.Artifact;
import rtc.pa.model.Attachment;
import rtc.pa.model.Link;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.utils.ProgressMonitor;

public class WorkItemHelper {

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

	public static String updateWorkItem(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, String dir,
			Project p, Task task) {

		String result;
		//
		// links
		//
		monitor.out("About to update work item " + task.getId() + " (old ID)");
		IWorkItem wi = (IWorkItem) task.getExternalObject();
		if (null == wi) {
			monitor.out("SHOULD NOT HAPPEN: null work item as the external object for this migration task "
					+ task.getId() + " (old Id) " + task.getExternalId());
			return null;
		}
		IWorkItemHandle wiH = (IWorkItemHandle) wi.getItemHandle();
		try {
			wiCopier.connect(wiH, IWorkItem.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "impossible to initialize a work item copy";
		}
		try {
			WorkItemWorkingCopy wc = wiCopier.getWorkingCopy(wiH);
			result = createLinks(wi, wc, task, monitor);
			if (null != result)
				return result;
			result = createArtifacts(wi, wc, task, monitor);
			if (null != result)
				return result;
			result = createAttachments(pa, wiClient, wiCommon, wi, wc, task, monitor, dir);
			if (null != result)
				return result;
			result = createApprovals(pa, wiClient, wiCommon, wi, wc, task, monitor);
			if (null != result)
				return result;
			IDetailedStatus s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("error updating work item " + wc.getWorkItem().getId());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ("error when creating work item links, etc.");
		} finally {
			wiCopier.disconnect(wiH);
		}
		monitor.out("\tupdated work item " + task.getId() + " (old ID).");
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
					return "problem while saving attachment for file " + filename;
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

	private static String createApprovals(IProjectArea pa, IWorkItemClient wiClient, IWorkItemCommon wiCommon,
			IWorkItem wi, WorkItemWorkingCopy wc, Task task, ProgressMonitor monitor) {

		for (Approval a : task.getApprovals()) {

		}
		return null;
	}

}
