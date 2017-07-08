package rtc.pa.write.plain;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IProcessItem;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.ICategory;

import rtc.model.Category;
import rtc.model.Iteration;
import rtc.model.Line;
import rtc.model.Project;
import rtc.utils.ProgressMonitor;

public class WriteHelper {

	public static String createCategory(IProjectArea pa, IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p,
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

	public static String createLine(IProjectArea pa, IProcessItemService service, ProgressMonitor monitor, Project p,
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

	public static String createIteration(IProjectArea pa, IProcessItemService service, ProgressMonitor monitor,
			Project p, Line line, Iteration parent, Iteration ite) {

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

	public static String setLineCurrent(IProjectArea pa, IProcessItemService service, ProgressMonitor monitor,
			Line line) {

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

}
