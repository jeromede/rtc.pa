package rtc.pa.write;

import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IProcessItem;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;

import rtc.pa.model.Iteration;
import rtc.pa.model.Line;
import rtc.pa.model.Project;
import rtc.pa.utils.ProgressMonitor;

public class TimelineHelper {
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
			devLine.setCurrentIteration((IIteration) line.getCurrent().getExternalObject());
		}
		try {
			IProjectArea pac = (IProjectArea) pa.getWorkingCopy();
			devLine.setProjectArea(pac);
			pac.addDevelopmentLine(devLine);
			if (line.isProjectLine()) {
				pac.setProjectDevelopmentLine(devLine);
			}
			IProcessItem[] savedItems = service.save(new IProcessItem[] { devLine, pac }, monitor);
			line.setExternalObject(devLine.getId(), savedItems[0]);
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
			IDevelopmentLine devLine = (IDevelopmentLine) line.getExternalObject();
			IDevelopmentLine devLineC = (IDevelopmentLine) service.getMutableCopy(devLine);
			iterationC.setDevelopmentLine(devLineC);
			if (null == parent) {
				devLineC.addIteration(iterationC);
				IProcessItem[] savedItems = service.save(new IProcessItem[] { devLineC, iterationC }, monitor);
				devLine = (IDevelopmentLine) savedItems[0];
				iteration = (IIteration) savedItems[1];
				line.setExternalObject(devLine.getId(), devLine);
				ite.setExternalObject(iteration.getId(), iteration);
				monitor.out("\titeration \"" + ite.getName() + "\" in line \"" + line.getName() + "\"" + '\n' + devLine
						+ '\n' + iteration);
			} else {
				IIteration parentIteration = (IIteration) parent.getExternalObject();
				IIteration parentIterationC = (IIteration) service.getMutableCopy(parentIteration);
				iterationC.setParent(parentIterationC);
				parentIterationC.addChild(iterationC);
				IProcessItem[] savedItems = service.save(new IProcessItem[] { devLineC, parentIterationC, iterationC },
						monitor);
				devLine = (IDevelopmentLine) savedItems[0];
				parentIteration = (IIteration) savedItems[1];
				iteration = (IIteration) savedItems[2];
				line.setExternalObject(devLine.getId(), devLine);
				parent.setExternalObject(parentIteration.getId(), parentIteration);
				ite.setExternalObject(iteration.getId(), iteration);
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
		IDevelopmentLine devLine = (IDevelopmentLine) line.getExternalObject();
		IIteration iteration = (IIteration) line.getCurrent().getExternalObject();
		try {
			IDevelopmentLine devLineC = (IDevelopmentLine) service.getMutableCopy(devLine);
			IIteration iterationC = (IIteration) service.getMutableCopy(iteration);
			devLineC.setCurrentIteration(iterationC);
			IProcessItem[] savedItems = service.save(new IProcessItem[] { devLineC, iterationC }, monitor);
			devLine = (IDevelopmentLine) savedItems[0];
			iteration = (IIteration) savedItems[1];
			line.setExternalObject(devLine.getId(), devLine);
			ite.setExternalObject(iteration.getId(), iteration);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error while setting current iteration \"" + ite.getName() + "\" in line\"" + line.getName() + "\"";
		}
		monitor.out(
				"Just set development line \"" + line.getName() + "\" current iteration to \"" + ite.getName() + "\"");
		return null;
	}

}