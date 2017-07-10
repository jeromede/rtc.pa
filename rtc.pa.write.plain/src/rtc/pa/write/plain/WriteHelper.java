package rtc.pa.write.plain;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

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
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;

import rtc.model.Category;
import rtc.model.Iteration;
import rtc.model.Line;
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
			monitor.out("\tcategory already exists " + category.getCategoryId().getInternalRepresentation());
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
						"\tjust created subcategory \"" + category.getCategoryId().getInternalRepresentation() + "\"");
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
		monitor.out("\tjust created development line \"" + line.getName() + "\"");
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
		monitor.out("\tjust created iteration \"" + ite.getName() + "\"");
		for (Iteration children : ite.getIterations()) {
			createIteration(pa, service, monitor, p, line, ite, children);
		}
		return null;
	}

	static String setLineCurrent(IProjectArea pa, IProcessItemService service, ProgressMonitor monitor, Line line) {

		Iteration ite = line.getCurrent();
		if (null == ite) {
			monitor.out("\tno current iteration has been set for development line " + line.getName() + "\"");
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
		monitor.out("\tjust set development line \"" + line.getName() + "\" current iteration to \"" + ite.getName()
				+ "\"");
		return null;
	}

	static String createWorkItem(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			Task task) {

		Collection<TaskVersion> versions = task.getHistory();
		TaskVersion firstVersion = null;
		for (TaskVersion v : versions) {
			firstVersion = v;
			break;
		}
		if (null == firstVersion) {
			return null;
		}
		IContributor creator = (IContributor) task.getCreator().getTargetObject();
		IWorkItemType type = (IWorkItemType) firstVersion.getType().getTargetObject();
		IWorkItemHandle wiHandle;
		try {
			wiHandle = wiCopier.connectNew(type, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("error while creating work item");
		}
		WorkItemWorkingCopy wc;
		IWorkItem wi = null;
		IDetailedStatus s;
		IWorkItemType newType;
		for (TaskVersion v : versions) {
			if (null == wi) {
				try {
					wiHandle = wiCopier.connectNew(type, monitor);
					wc = wiCopier.getWorkingCopy(wiHandle);
					wi = wc.getWorkItem();
					wi.setCreator(creator);
					wi.setCreationDate(new Timestamp(task.getCreation().getTime()));
					WriteHelper.updateWorkItemVersion(repo, pa, wiClient, wiCommon, wiCopier, monitor, p, wi, v);
					s = wc.save(monitor);
					if (!s.isOK()) {
						s.getException().printStackTrace();
						return ("error updating new work item");
					}
				} catch (TeamRepositoryException e) {
					e.printStackTrace();
					return ("error when creating work item");
				} finally {
					wiCopier.disconnect(wi);
				}
			} else {
				newType = (IWorkItemType) v.getTargetObject();
				if (!v.isOfType(type.getIdentifier())) {
					try {
						wiCommon.updateWorkItemType(wi, newType, type, monitor);
					} catch (TeamRepositoryException e) {
						e.printStackTrace();
						return ("error while changing the type of the work item");
					}
				}
				try {
					wiCopier.connect(wi, IWorkItem.FULL_PROFILE, monitor);
					wc = wiCopier.getWorkingCopy(wi);
					wi = wc.getWorkItem();
					WriteHelper.updateWorkItemVersion(repo, pa, wiClient, wiCommon, wiCopier, monitor, p, wi, v);
					s = wc.save(monitor);
					if (!s.isOK()) {
						s.getException().printStackTrace();
						return ("error updating new work item");
					}
				} catch (TeamRepositoryException e) {
					e.printStackTrace();
					return ("error while connecting to work item");
				} finally {
					wiCopier.disconnect(wi);
				}
			}
			try {
				wi = (IWorkItem) repo.itemManager().fetchCompleteItem(wi, IItemManager.DEFAULT, monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return ("error fetching created work item");
			}
		}
		task.setTargetObject(wi.getItemId().getUuidValue(), wi);
		System.out.println("Created workitem: " + wi.getId());
		return null;

	}

	static String updateWorkItemVersion(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p,
			IWorkItem wi, TaskVersion version) {

		monitor.out("Create new work item version for (summary): " + version.getSummary());
		IAttribute modifierInSource = null;
		IAttribute modifiedInSource = null;
		List<IAttributeHandle> customAttributes = wi.getCustomAttributes();
		try {
			modifierInSource = wiClient.findAttribute(pa, "rtc.pa.modifier", monitor);
			monitor.out(modifierInSource.getIdentifier() + " : " + modifierInSource.getAttributeType());
			modifiedInSource = wiClient.findAttribute(pa, "rtc.pa.modified", monitor);
			monitor.out(modifiedInSource.getIdentifier() + " : " + modifiedInSource.getAttributeType());
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("can't find special attributes to reflect modification in source");
		}
		if (null != modifiedInSource) {
			wi.setValue(modifiedInSource, new Timestamp(version.getModified().getTime()));
		}
		if (null != modifierInSource) {
			wi.setValue(modifierInSource, (IContributor) version.getModifier().getTargetObject());
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

}
