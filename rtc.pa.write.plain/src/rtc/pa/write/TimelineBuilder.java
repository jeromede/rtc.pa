/*
 * Copyright (c) 2017,2018,2019 jeromede@fr.ibm.com
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

import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.client.workingcopies.IWorkingCopyManager;
import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IProcessItem;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;

import rtc.pa.model.Iteration;
import rtc.pa.model.Line;
import rtc.pa.model.Project;
import rtc.pa.utils.ProgressMonitor;

public class TimelineBuilder {

	public static void createTimelines(ITeamRepository repo, IProjectArea pa, IProcessItemService service,
			IWorkingCopyManager pCopier, ProgressMonitor monitor, Project p) throws TeamRepositoryException {

		p.setExternalObject(pa.getName(), pa);
		for (Line line : p.getLines()) {
			createLine(p, line, service, monitor);
			for (Iteration ite : line.getIterations()) {
				createIteration(line, null, ite, service, monitor);
			}
		}
	}

	private static void createLine(Project p, Line line, IProcessItemService service, ProgressMonitor monitor)
			throws TeamRepositoryException {

		IDevelopmentLine devLine = (IDevelopmentLine) IDevelopmentLine.ITEM_TYPE.createItem();
		devLine.setId(line.getAlternateId());
		devLine.setName(line.getName());
		devLine.setStartDate(line.getStarts());
		devLine.setEndDate(line.getEnds());
		IProjectArea pa = (IProjectArea) p.getExternalObject();
		pa = (IProjectArea) pa.getWorkingCopy();
		devLine.setProjectArea(pa);
		pa.addDevelopmentLine(devLine);
		if (line.isProjectLine()) {
			pa.setProjectDevelopmentLine(devLine);
		}
		IProcessItem[] items = service.save(new IProcessItem[] { pa, devLine }, monitor);
		pa = (IProjectArea) items[0];
		devLine = (IDevelopmentLine) items[1];
		p.setExternalObject(pa.getName(), pa);
		line.setExternalObject(devLine.getId(), devLine);
		monitor.out("Just created development line " + devLine.getId() + " (" + devLine.getName() + ')');
	}

	private static void createIteration(Line line, Iteration parent, Iteration ite, IProcessItemService service,
			ProgressMonitor monitor) throws TeamRepositoryException {

		IIteration iteration = (IIteration) IIteration.ITEM_TYPE.createItem();
		iteration.setId(ite.getAlternateId());
		iteration.setName(ite.getName());
		iteration.setStartDate(ite.getStarts());
		iteration.setEndDate(ite.getEnds());
		iteration.setIterationType(null);
		iteration.setHasDeliverable(ite.hasDeliverable());
		IDevelopmentLine devLine = (IDevelopmentLine) line.getExternalObject();
		devLine = (IDevelopmentLine) devLine.getWorkingCopy();
		if (null == parent) {
			devLine.addIteration(iteration);
			iteration.setDevelopmentLine(devLine);
			IProcessItem[] items = service.save(new IProcessItem[] { devLine, iteration }, monitor);
			devLine = (IDevelopmentLine) items[0];
			iteration = (IIteration) items[1];
			line.setExternalObject(devLine.getId(), devLine);
			ite.setExternalObject(iteration.getId(), iteration);
			monitor.out("Just created iteration " + iteration.getId() + " (" + iteration.getName()
					+ ") in development line " + devLine.getId() + " (" + devLine.getName() + ')');
		} else {
			iteration.setDevelopmentLine(devLine);
			IIteration parentIteration = (IIteration) parent.getExternalObject();
			parentIteration = (IIteration) parentIteration.getWorkingCopy();
			parentIteration.addChild(iteration);
			iteration.setParent(parentIteration);
			IProcessItem[] items = service.save(new IProcessItem[] { devLine, parentIteration, iteration }, monitor);
			devLine = (IDevelopmentLine) items[0];
			parentIteration = (IIteration) items[1];
			iteration = (IIteration) items[2];
			line.setExternalObject(devLine.getId(), devLine);
			parent.setExternalObject(parentIteration.getId(), parentIteration);
			ite.setExternalObject(iteration.getId(), iteration);
			monitor.out("Just created iteration " + iteration.getId() + " (" + iteration.getName()
					+ ") children of iteration " + parentIteration.getId() + " (" + iteration.getName() + ')');

		}
		if (line.getCurrent() == ite) {
			devLine = (IDevelopmentLine) devLine.getWorkingCopy();
			iteration = (IIteration) iteration.getWorkingCopy();
			devLine.setCurrentIteration(iteration);
			IProcessItem[] items = service.save(new IProcessItem[] { devLine, iteration }, monitor);
			devLine = (IDevelopmentLine) items[0];
			iteration = (IIteration) items[1];
			line.setExternalObject(devLine.getId(), devLine);
			ite.setExternalObject(iteration.getId(), iteration);
		}
		for (Iteration children : ite.getIterations()) {
			createIteration(line, ite, children, service, monitor);
		}
	}

}
