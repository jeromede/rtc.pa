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

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;

import rtc.pa.utils.ProgressMonitor;

public class StateHelper {

	public static String action(IProjectArea pa, IWorkItemCommon wiCommon, ProgressMonitor monitor, String type,
			String state0, String state1) throws TeamRepositoryException {

		//
		// This is an expansive shortcut
		// TODO: refactor workitem state change (create a model, read matching
		// file... still some design to think about)
		//

		//
		// State matching: hack for JazzHub -> RTC 6.0.x (some state ids have
		// changed)
		//
		String begin = state0;
		String end = state1;
		if (1 == state0.length() || 1 == state1.length()) {
			if (type.equals("defect")) {
				begin = "com.ibm.team.workitem.defectWorkflow.state.s" + state0;
				end = "com.ibm.team.workitem.defectWorkflow.state.s" + state1;
			} else if (type.equals("task")) {
				begin = "com.ibm.team.workitem.taskWorkflow.state.s" + state0;
				end = "com.ibm.team.workitem.taskWorkflow.state.s" + state1;
			}
		}
		//
		// Search workflow
		//
		IWorkflowInfo wf = wiCommon.getWorkflow(type, pa, monitor);
		Identifier<IState>[] states = wf.getAllStateIds();
		for (Identifier<IState> s : states) {
			if (s.getStringIdentifier().equals(begin)) {
				Identifier<IWorkflowAction>[] actions = wf.getActionIds(s);
				for (Identifier<IWorkflowAction> a : actions) {
					if (wf.getActionResultState(a).getStringIdentifier().equals(end)) {
						return a.getStringIdentifier();
					}
				}
			}
		}
		return null;
	}

	public static Identifier<IState> stateId(IProjectArea pa, IWorkItemCommon wiCommon, ProgressMonitor monitor,
			String type, String state) throws TeamRepositoryException {

		monitor.out("\t\tlooking for state");
		//
		// State matching: hack for JazzHub -> RTC 6.0.x (some state ids have
		// changed)
		//
		String begin = state;
		if (1 == state.length()) {
			if (type.equals("defect")) {
				begin = "com.ibm.team.workitem.defectWorkflow.state.s" + state;
			} else if (type.equals("task")) {
				begin = "com.ibm.team.workitem.taskWorkflow.state.s" + state;
			}
		}
		//
		// Search workflow
		//
		IWorkflowInfo wf = wiCommon.getWorkflow(type, pa, monitor);
		Identifier<IState>[] states = wf.getAllStateIds();
		for (Identifier<IState> s : states) {
			monitor.out("\t\tstate? " + s.getStringIdentifier());
			if (s.getStringIdentifier().equals(begin)) {
				return s;
			}
		}
		return null;
	}

}
