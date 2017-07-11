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

package rtc.pa.write.plain;

import java.util.List;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.workflow.IWorkflowAction;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;

import rtc.utils.ProgressMonitor;

public class StateHelper {

	public static String readWorkItemTypes(IProjectArea pa, IWorkItemClient wiClient, IWorkItemCommon wiCommon,
			ProgressMonitor monitor) {

		monitor.out("Reading work item types...");
		IWorkflowInfo wf;
		List<IWorkItemType> allWorkItemTypes;
		try {
			allWorkItemTypes = wiClient.findWorkItemTypes(pa, monitor);
			for (IWorkItemType t : allWorkItemTypes) {
				monitor.out("\t" + t.getDisplayName() + " (" + t.getIdentifier() + ')');
				wf = wiCommon.getWorkflow(t.getIdentifier(), pa, monitor);
				Identifier<IState>[] states = wf.getAllStateIds();
				for (Identifier<IState> state : states) {
					monitor.out("\t\tstate: " + state.getStringIdentifier());
					Identifier<IWorkflowAction>[] actions = wf.getActionIds(state);
					for (Identifier<IWorkflowAction> action : actions) {
						monitor.out("\t\t\taction: " + action.getStringIdentifier());
						Identifier<IState> result = wf.getActionResultState(action);
						monitor.out("\t\t\t\t to state: " + result.getStringIdentifier());
					}
				}
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem while getting work item types";
		}
		return null;
	}

	public static Identifier<IWorkflowAction> action(IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, IWorkItemType type, String state0, String state1)
			throws TeamRepositoryException {

		//
		// Expansive shortcut
		// TODO: refactor workitem state change
		//
		IWorkflowInfo wf;
		wf = wiCommon.getWorkflow(type.getIdentifier(), pa, monitor);
		Identifier<IState>[] states = wf.getAllStateIds();
		Identifier<IState> begin = null;
		Identifier<IWorkflowAction> foundAction = null;
		for (Identifier<IState> state : states) {
			if (state.getStringIdentifier().equals(state0)) {
				begin = state;
				break;
			}
		}
		if (null == begin) {
			return null;
		}
		Identifier<IWorkflowAction>[] actions = wf.getActionIds(begin);
		for (Identifier<IWorkflowAction> action : actions) {
			Identifier<IState> result = wf.getActionResultState(action);
			if (result.getStringIdentifier().equals(state1)) {
				foundAction = action;
				break;
			}
		}
		return foundAction;
	}
}
