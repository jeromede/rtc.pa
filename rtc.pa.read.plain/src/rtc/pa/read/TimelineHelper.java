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
			monitor.out("\tjust added development line " + devLine.getName());
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
			monitor.out(prefix + "\t\tcurrent iteration!");
			line.setCurrent(ite);
		}
		monitor.out(prefix + "\tjust added iteration " + iteration.getName());
		for (IIterationHandle children : iteration.getChildren()) {
			readIteration(devLine, currentIterationHandle, children, auditableClient, itemManager, prefix + "\t",
					monitor, p, line, ite);
		}
		return null;
	}

}
