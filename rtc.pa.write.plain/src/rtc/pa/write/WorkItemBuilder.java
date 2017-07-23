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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;

import rtc.pa.model.Comment;
import rtc.pa.model.Member;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskVersion;
import rtc.pa.utils.ProgressMonitor;

public class WorkItemBuilder {

	static String createUpdateWorkItemWithAllVersions(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor,
			Map<Integer, Task> tasks, Project p, Task task) {

		String result;
		monitor.out("About to create/update work item (source ID): " + task.getId() + " (old id)");
		boolean create = (null == task.getExternalObject());
		Collection<TaskVersion> versions = task.getHistory();

		TaskVersion firstVersion = null;
		for (TaskVersion v : versions) {
			firstVersion = v;
			break;
		}
		if (null == firstVersion) {
			monitor.out("\tno first version!");
			return null;
		}
		IWorkItemType type = (IWorkItemType) firstVersion.getType().getExternalObject();
		IWorkItemType previousType = null;
		String state;
		String previousState = null;
		IWorkItemHandle wiH;
		WorkItemWorkingCopy wc;
		Map<Timestamp, Comment> comments = new HashMap<Timestamp, Comment>();
		String action;
		IDetailedStatus s;
		Identifier<IState> stateId;
		IWorkItem wi;
		if (create) {
			wi = null;
		} else {
			wi = (IWorkItem) task.getExternalObject();
		}
		try {
			if (create) {
				wiH = wiCopier.connectNew(type, monitor);
			} else {
				wiH = (IWorkItemHandle) wi.getItemHandle();
				wiCopier.connect(wiH, IWorkItem.FULL_PROFILE, monitor);
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "impossible to initialize a new work item";
		}
		wc = wiCopier.getWorkingCopy(wiH);
		wi = wc.getWorkItem();
		try {
			if (create) {
				wi.setCreator(getC(repo, task.getCreator()));
				wi.setCreationDate(new Timestamp(task.getCreation().getTime()));
			}
			for (TaskVersion v : versions) {
				type = (IWorkItemType) v.getType().getExternalObject();
				state = v.getState();

				if (null != previousType) {
					if (!type.getIdentifier().equals(previousType.getIdentifier())) {
						monitor.out("\tchanging work item type");
						wiCommon.updateWorkItemType(wi, type, previousType, monitor);
					}
				}

				result = WorkItemCopyBuilder.fillWorkItemVersion(repo, pa, wiClient, wiCommon, monitor, tasks, p, wi, v,
						comments);
				if (null != result)
					return result;

				if (null != previousState) {
					if (!state.equals(previousState)) {
						monitor.out("\tfrom state " + type.getIdentifier() + ":" + previousState);
						monitor.out("\t  to state " + type.getIdentifier() + ":" + state);
						action = null;
						try {
							action = StateHelper.action(pa, wiCommon, monitor, type.getIdentifier(), previousState,
									state);
						} catch (TeamRepositoryException e) {
							e.printStackTrace();
							return "problem while searching action to trigger";
						}
						if (null == action) {
							stateId = StateHelper.stateId(pa, wiCommon, monitor, type.getIdentifier(), state);
							if (null == stateId) {
								return "couldn't find state " + state + " for type " + type.getIdentifier();
							}
							monitor.out("\t    force state (TOO BAD) to become:");
							forceState(wi, stateId);
							monitor.out("\t    " + stateId.getStringIdentifier());
						} else {
							monitor.out("\t    action:");
							wc.setWorkflowAction(action);
							monitor.out("\t    " + action);
						}
					}
				}

				s = wc.save(monitor);
				if (!s.isOK()) {
					s.getException().printStackTrace();
					// return ("error adding new work item version");
					monitor.out("error adding new work item version, but continue anyway...");
				}
				previousState = state;
				previousType = type;
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("error when creating work item");
		} finally {
			wiCopier.disconnect(wi);
		}
		try {
			wi = (IWorkItem) repo.itemManager().fetchCompleteItem(wi, IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("error fetching created work item");
		}
		task.setExternalObject("" + wi.getId(), wi);
		monitor.out("\tattached external object " + wi.getItemId().getUuidValue() + ", <" + wi.getId() + '>');
		System.out.println("Just created work item: " + wi.getId() + " (new id)");
		return null;
	}

	static String createMinimalWorkItemWithLinks(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			Task task, String dir) {

		String result;
		monitor.out("About to create minimal work item (source ID), with links: " + task.getId() + " (old id)");
		Collection<TaskVersion> versions = task.getHistory();
		TaskVersion firstVersion = null;
		for (TaskVersion v : versions) {
			firstVersion = v;
			break;
		}
		if (null == firstVersion) {
			monitor.out("\tno first version!");
			return null;
		}
		IWorkItemType type = (IWorkItemType) firstVersion.getType().getExternalObject();
		IWorkItemHandle wiH;
		WorkItemWorkingCopy wc;
		IWorkItem wi = null;
		IDetailedStatus s;
		try {
			wiH = wiCopier.connectNew(type, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "impossible to initialize a new work item";
		}
		wc = wiCopier.getWorkingCopy(wiH);
		wi = wc.getWorkItem();
		try {
			wi.setCreator(getC(repo, task.getCreator()));
			wi.setCreationDate(new Timestamp(task.getCreation().getTime()));
			type = (IWorkItemType) firstVersion.getType().getExternalObject();

			result = WorkItemCopyBuilder.fillMinimalWorkItemVersion(repo, pa, wiClient, wiCommon, monitor, p, wi,
					firstVersion);
			if (null != result)
				return result;

			result = WorkItemCopyBuilder.updateWorkItemCopyWithLinks(repo, pa, wiClient, wiCommon, wiCopier, monitor,
					dir, wi, wiH, p, task);
			if (null != result)
				return result;

			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("error creating minimal new work item with its links, etc.");
			}

		} finally {
			wiCopier.disconnect(wi);
		}
		try {
			wi = (IWorkItem) repo.itemManager().fetchCompleteItem(wi, IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("error fetching created work item");
		}
		task.setExternalObject("" + wi.getId(), wi);
		monitor.out("\tattached external object " + wi.getItemId().getUuidValue() + ", <" + wi.getId() + '>');
		System.out.println("Just created minimal work item with links: " + wi.getId() + " (new id)");
		return null;
	}

	static IContributor getC(ITeamRepository repo, Member m) {
		IContributor c = null;
		if (null != m) {
			c = (IContributor) m.getExternalObject();
		}
		if (null == c) {
			c = repo.loggedInContributor();
		}
		return c;
	}

	@SuppressWarnings("deprecation")
	static void forceState(IWorkItem wi, Identifier<IState> stateId) {
		wi.setState2(stateId); // TOO BAD
	}

	static String updateWorkItemWithLinks(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			Task task, String dir) {

		String result;
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
			result = WorkItemCopyBuilder.updateWorkItemCopyWithLinks(repo, pa, wiClient, wiCommon, wiCopier, monitor,
					dir, wi, wiH, p, task);
			if (null != result)
				return result;
			IDetailedStatus s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("error updating work item with links, etc. " + wc.getWorkItem().getId());
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

}
