package rtc.pa.write;

import java.sql.Timestamp;
import java.util.Collection;

import com.ibm.team.foundation.common.text.XMLString;
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
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;

import rtc.pa.model.Category;
import rtc.pa.model.Member;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskVersion;
import rtc.pa.utils.ProgressMonitor;

public class WorkItemHelper {

	static String createWorkItem(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			Task task) {

		monitor.out("About to create work item (source ID): " + task.getId());
		Collection<TaskVersion> versions = task.getHistory();
		TaskVersion firstVersion = null;
		for (TaskVersion v : versions) {
			firstVersion = v;
			break;
		}
		if (null == firstVersion) {
			return null;
		}
		IWorkItemType type = (IWorkItemType) firstVersion.getType().getTargetObject();
		IWorkItemType previousType = null;
		String state;
		String previousState = null;
		IWorkItemHandle wiHandle;
		WorkItemWorkingCopy wc;
		IWorkItem wi = null;
		String action;
		IDetailedStatus s;
		Identifier<IState> stateId;
		try {
			wiHandle = wiCopier.connectNew(type, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "impossible to initialize a new work item";
		}
		wc = wiCopier.getWorkingCopy(wiHandle);
		wi = wc.getWorkItem();
		try {
			wi.setCreator(getC(repo, task.getCreator()));
			wi.setCreationDate(new Timestamp(task.getCreation().getTime()));
			for (TaskVersion v : versions) {
				type = (IWorkItemType) v.getType().getTargetObject();
				state = v.getState();
				CHANGE_TYPE: {
					if (null != previousType) {
						if (!type.getIdentifier().equals(previousType.getIdentifier())) {
							monitor.out("Changing work item type");
							wiCommon.updateWorkItemType(wi, type, previousType, monitor);
							// s = wc.save(monitor);
							// if (!s.isOK()) {
							// s.getException().printStackTrace();
							// return ("error changing work item type");
							// }
						}
					}
				}
				UPDATE: {
					updateWorkItemVersion(repo, pa, wiClient, wiCommon, wiCopier, monitor, p, wi, v);
					// s = wc.save(monitor);
					// if (!s.isOK()) {
					// s.getException().printStackTrace();
					// return ("error updating work item");
					// }
				}
				CHANGE_STATE: {
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
								wi.setState2(stateId); // TOO BAD (probably a
														// side effect of a
														// change of type)
							}
							monitor.out("\t    action: " + action);
							wc.setWorkflowAction(action);
							// s = wc.save(monitor);
							// if (!s.isOK()) {
							// s.getException().printStackTrace();
							// return ("error changing work item state");
							// }
						}
					}
				}
				// Save once instead:
				s = wc.save(monitor);
				if (!s.isOK()) {
					s.getException().printStackTrace();
					return ("error adding new work item version");
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
		task.setExternalObject(wi.getItemId().getUuidValue(), wi);
		System.out.println("Just created workitem: " + wi.getId());
		return null;
	}

	private static String updateWorkItemVersion(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			IWorkItem wi, TaskVersion version) {

		monitor.out("Create new work item version for (summary): " + version.getSummary());
		IAttribute modifierInSource = null;
		IAttribute modifiedInSource = null;
		// List<IAttributeHandle> customAttributes = wi.getCustomAttributes();
		try {
			modifierInSource = wiClient.findAttribute(pa, "rtc.pa.modifier", monitor);
			modifiedInSource = wiClient.findAttribute(pa, "rtc.pa.modified", monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("can't find special attributes to reflect modification in source");
		}
		if (null != modifiedInSource) {
			wi.setValue(modifiedInSource, new Timestamp(version.getModified().getTime()));
		}
		if (null != modifierInSource) {
			wi.setValue(modifierInSource, getC(repo, version.getModifier()));
		}
		if (null == version.getSummary()) {
			wi.setHTMLSummary(null);
		} else {
			wi.setHTMLSummary(XMLString.createFromXMLText(version.getSummary()));
		}
		if (null == version.getDescription()) {
			wi.setHTMLDescription(null);
		} else {
			wi.setHTMLDescription(XMLString.createFromXMLText(version.getDescription()));
		}
		// TODO
		// Identifier<IPriority> priority;
		// wi.setPriority(priority);
		Category cat = version.getCategory();
		ICategory category;
		if (null == cat) {
			try {
				category = wiCommon.findUnassignedCategory(pa, ICategory.FULL_PROFILE, monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "can't find /unassigned/ category";
			}
		} else {
			category = (ICategory) cat.getTargetObject();
		}
		wi.setCategory(category);
		return null;
	}

	static IContributor getC(ITeamRepository repo, Member m) {
		IContributor c = null;
		if (null != m) {
			c = (IContributor) m.getTargetObject();
		}
		if (null == c) {
			c = repo.loggedInContributor();
		}
		return c;
	}

}
