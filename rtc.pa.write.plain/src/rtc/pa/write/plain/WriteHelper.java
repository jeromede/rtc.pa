package rtc.pa.write.plain;

import java.sql.Timestamp;
import java.util.Collection;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IProcessItem;
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

import rtc.model.Category;
import rtc.model.Iteration;
import rtc.model.Line;
import rtc.model.Member;
import rtc.model.Project;
import rtc.model.Task;
import rtc.model.TaskVersion;
import rtc.utils.ProgressMonitor;

public class WriteHelper {

	static String createCategory(IProjectArea pa, IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p,
			Category cat) {

		ICategory category;
		if (null != cat.getTargetObject()) {
			category = (ICategory) cat.getTargetObject();
			monitor.out("Category already exists " + category.getCategoryId().getInternalRepresentation());
			return null;
		}
		if (null == cat.getParentId()) {
			try {
				category = wiCommon.createCategory(pa, cat.getName(), monitor);
				cat.setTargetObject(category.getCategoryId().getInternalRepresentation(), category);
				monitor.out("\tjust created category \"" + category.getCategoryId().getInternalRepresentation() + "\"");
				finaliseCategory(wiCommon, monitor, cat);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error creating target category for source category \"" + cat.getSourceUUID() + "\"";
			}
		} else {
			Category par = p.getCategory(cat.getParentId());
			String message = createCategory(pa, wiCommon, monitor, p, par);
			if (null != message) {
				return message;
			}
			try {
				category = wiCommon.createSubcategory((ICategory) par.getTargetObject(), cat.getName(), monitor);
				cat.setTargetObject(category.getCategoryId().getInternalRepresentation(), category);
				monitor.out(
						"Just created subcategory \"" + category.getCategoryId().getInternalRepresentation() + "\"");
				finaliseCategory(wiCommon, monitor, cat);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error creating target subcategory for source category \"" + cat.getSourceUUID() + "\"";
			}
		}
		return null;
	}

	private static void finaliseCategory(IWorkItemCommon wiCommon, ProgressMonitor monitor, Category cat)
			throws TeamRepositoryException {
		ICategory category = (ICategory) cat.getTargetObject();
		category.setHTMLDescription(XMLString.createFromXMLText(cat.getDescription()));
		wiCommon.saveCategory(category, monitor);
	}

	static String createLine(IProjectArea pa, IProcessItemService service, ProgressMonitor monitor, Project p,
			Line line) {

		IDevelopmentLine devLine = service.createDevelopmentLine();
		devLine.setId(line.getAlternateId());
		if (null != line.getName()) {
			devLine.setName(line.getName());
		}
		if (null != line.getEnds()) {
			devLine.setEndDate(line.getEnds());
		}
		if (null != line.getStarts()) {
			devLine.setStartDate(line.getStarts());
		}
		if (null != line.getCurrent()) {
			devLine.setCurrentIteration((IIteration) line.getCurrent().getTargetObject());
		}
		try {
			IProjectArea pac = (IProjectArea) pa.getWorkingCopy();
			devLine.setProjectArea(pac);
			pac.addDevelopmentLine(devLine);
			if (line.isProjectLine()) {
				pac.setProjectDevelopmentLine(devLine);
			}
			IProcessItem[] savedItems = service.save(new IProcessItem[] { devLine, pac }, monitor);
			line.setTargetObject(devLine.getId(), savedItems[0]);
		} catch (Exception e) {
			e.printStackTrace();
			return "error while adding development line \"" + line.getName() + "\" to project area";
		}
		monitor.out("Just created development line \"" + line.getName() + "\"");
		return null;
	}

	static String createIteration(IProjectArea pa, IProcessItemService service, ProgressMonitor monitor, Project p,
			Line line, Iteration parent, Iteration ite) {

		IIteration iterationC = service.createIteration();
		iterationC.setId(ite.getAlternateId());
		if (null != ite.getName()) {
			iterationC.setName(ite.getName());
		}
		if (null != ite.getStarts()) {
			iterationC.setStartDate(ite.getStarts());
		}
		if (null != ite.getEnds()) {
			iterationC.setEndDate(ite.getEnds());
		}
		try {
			IIteration iteration = null;
			IDevelopmentLine devLine = (IDevelopmentLine) line.getTargetObject();
			IDevelopmentLine devLineC = (IDevelopmentLine) service.getMutableCopy(devLine);
			iterationC.setDevelopmentLine(devLineC);
			if (null == parent) {
				devLineC.addIteration(iterationC);
				IProcessItem[] savedItems = service.save(new IProcessItem[] { devLineC, iterationC }, monitor);
				devLine = (IDevelopmentLine) savedItems[0];
				iteration = (IIteration) savedItems[1];
				line.setTargetObject(devLine.getId(), devLine);
				ite.setTargetObject(iteration.getId(), iteration);
				monitor.out("\titeration \"" + ite.getName() + "\" in line \"" + line.getName() + "\"" + '\n' + devLine
						+ '\n' + iteration);
			} else {
				IIteration parentIteration = (IIteration) parent.getTargetObject();
				IIteration parentIterationC = (IIteration) service.getMutableCopy(parentIteration);
				iterationC.setParent(parentIterationC);
				parentIterationC.addChild(iterationC);
				IProcessItem[] savedItems = service.save(new IProcessItem[] { devLineC, parentIterationC, iterationC },
						monitor);
				devLine = (IDevelopmentLine) savedItems[0];
				parentIteration = (IIteration) savedItems[1];
				iteration = (IIteration) savedItems[2];
				line.setTargetObject(devLine.getId(), devLine);
				parent.setTargetObject(parentIteration.getId(), parentIteration);
				ite.setTargetObject(iteration.getId(), iteration);
				monitor.out("\titeration \"" + ite.getName() + "\" in parent iteration \"" + parent.getName() + "\"");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "error while adding iteration \"" + ite.getName() + "\"";
		}
		monitor.out("Just created iteration \"" + ite.getName() + "\"");
		for (Iteration children : ite.getIterations()) {
			createIteration(pa, service, monitor, p, line, ite, children);
		}
		return null;
	}

	static String setLineCurrent(IProjectArea pa, IProcessItemService service, ProgressMonitor monitor, Line line) {

		Iteration ite = line.getCurrent();
		if (null == ite) {
			monitor.out("No current iteration has been set for development line " + line.getName() + "\"");
			return null;
		}
		IDevelopmentLine devLine = (IDevelopmentLine) line.getTargetObject();
		IIteration iteration = (IIteration) line.getCurrent().getTargetObject();
		try {
			IDevelopmentLine devLineC = (IDevelopmentLine) service.getMutableCopy(devLine);
			IIteration iterationC = (IIteration) service.getMutableCopy(iteration);
			devLineC.setCurrentIteration(iterationC);
			IProcessItem[] savedItems = service.save(new IProcessItem[] { devLineC, iterationC }, monitor);
			devLine = (IDevelopmentLine) savedItems[0];
			iteration = (IIteration) savedItems[1];
			line.setTargetObject(devLine.getId(), devLine);
			ite.setTargetObject(iteration.getId(), iteration);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error while setting current iteration \"" + ite.getName() + "\" in line\"" + line.getName() + "\"";
		}
		monitor.out(
				"Just set development line \"" + line.getName() + "\" current iteration to \"" + ite.getName() + "\"");
		return null;
	}

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
							s = wc.save(monitor);
							if (!s.isOK()) {
								s.getException().printStackTrace();
								return ("error changing work item type");
							}
						}
					}
				}
				UPDATE: {
					updateWorkItemVersion(repo, pa, wiClient, wiCommon, wiCopier, monitor, p, wi, v);
					s = wc.save(monitor);
					if (!s.isOK()) {
						s.getException().printStackTrace();
						return ("error updating work item");
					}
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
							s = wc.save(monitor);
							if (!s.isOK()) {
								s.getException().printStackTrace();
								return ("error changing work item state");
							}
						}
					}
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
		task.setTargetObject(wi.getItemId().getUuidValue(), wi);
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
