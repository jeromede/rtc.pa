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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.team.links.common.IItemReference;
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
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;
import com.ibm.team.workitem.common.model.WorkItemLinkTypes;

import rtc.pa.model.Link;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.utils.ProgressMonitor;

public class LinkHelper {

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

	public static String createLinks(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			Task task) {

		//
		// links
		//
		monitor.out("About to create links from work item " + task.getId() + " (old ID)");
		IWorkItem wi = (IWorkItem) task.getExternalObject();
		if (null == wi) {
			monitor.out(
					"SHOUD NOT HAPPEN: null wi as external object for migration task " + task.getId() + " (old Id) " + task.getExternalId());
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
			IWorkItem otherWi;
			IItemReference reference;
			IEndPointDescriptor endpoint;
			for (Link link : task.getLinks()) {
				endpoint = linkTypes.get(link.getType());
				if (null == endpoint)
					continue;
				if (null == link.getTarget())
					continue;
				otherWi = (IWorkItem) link.getTarget().getExternalObject();
				if (null == otherWi)
					continue;
				reference = IReferenceFactory.INSTANCE.createReferenceToItem(otherWi.getItemHandle());
				monitor.out(
						"\tabout to create link " + link.getType() + " from " + wi.getId() + " to " + otherWi.getId());
				wc.getReferences().add(endpoint, reference);
				monitor.out("\tcreated link " + link.getType() + " from " + wi.getId() + " to " + otherWi.getId());
			}
			IDetailedStatus s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("error adding links to new work item " + wc.getWorkItem().getId());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ("error when creating work item link");
		} finally {
			wiCopier.disconnect(wiH);
		}
		monitor.out("\tlinks from work item " + task.getId() + " (old ID) created.");
		return null;
	}

}
