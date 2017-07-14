package rtc.pa.read;

import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IDevelopmentLineHandle;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.common.model.ItemProfile;

import rtc.pa.model.Iteration;
import rtc.pa.model.Line;
import rtc.pa.model.Project;
import rtc.pa.utils.ProgressMonitor;

public class TimelineHelper {

	static String readDevelopmentLines(ITeamRepository repo, IProjectArea pa, IAuditableClient auditableClient,
			IItemManager itemManager, ProgressMonitor monitor, Project p) {

		monitor.out("Now reading development lines...");
		String message;
		Line line;
		IDevelopmentLineHandle[] devLines = pa.getDevelopmentLines();
		IDevelopmentLine devLine;
		IDevelopmentLineHandle current = pa.getProjectDevelopmentLine();
		for (IDevelopmentLineHandle devLineHandle : devLines) {
			//
			// Development line
			//
			try {
				devLine = auditableClient.resolveAuditable(devLineHandle, ItemProfile.DEVELOPMENT_LINE_DEFAULT,
						monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error resolving development line handle";
			}
			if (devLine.isArchived()) {
				continue;
			}
			line = new Line(//
					devLine.getItemId().getUuidValue(), //
					devLine.getId(), //
					devLine.getName(), //
					devLine.getStartDate(), //
					devLine.getEndDate(), //
					devLine.getItemId().getUuidValue().equals(current.getItemId().getUuidValue()));
			p.putLine(line);
			monitor.out("\tjust added development line " + devLine.getName() + '\n' + devLine);
			//
			// Iterations
			//
			for (IIterationHandle iterationHandle : devLine.getIterations()) {
				message = readIteration(devLine, devLine.getCurrentIteration(), iterationHandle, auditableClient,
						itemManager, "\t", monitor, p, line, null);
				if (null != message) {
					return "error reading iteration " + iterationHandle;
				}
			}
		}
		monitor.out("... development lines read.");
		return null;
	}

	static String readIteration(IDevelopmentLine devLine, IIterationHandle currentIterationHandle,
			IIterationHandle iterationHandle, IAuditableClient auditableClient, IItemManager itemManager, String prefix,
			ProgressMonitor monitor, Project p, Line line, Iteration parent) {

		IIteration iteration;
		Iteration ite;
		try {
			iteration = auditableClient.resolveAuditable(iterationHandle, ItemProfile.ITERATION_DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error resolving iteration handle";
		}
		if (iteration.isArchived()) {
			return null;
		}
		ite = new Iteration(//
				iteration.getItemId().getUuidValue(), //
				iteration.getName(), //
				iteration.getId(), //
				iteration.getLabel(), //
				iteration.getDescription().getSummary(), //
				iteration.getStartDate(), //
				iteration.getEndDate());
		if (null == parent) {
			p.putIteration(line, ite);
		} else {
			p.putIteration(line, parent, ite);
		}
		if (iterationHandle.sameItemId(currentIterationHandle)) {
			monitor.out(prefix + "\tcurrent iteration!");
			line.setCurrent(ite);
		}
		monitor.out(prefix + "\tjust added iteration " + iteration.getName() + '\n' + iteration);
		for (IIterationHandle children : iteration.getChildren()) {
			readIteration(devLine, currentIterationHandle, children, auditableClient, itemManager, prefix + "\t",
					monitor, p, line, ite);
		}
		return null;
	}

}