package rtc.pa.read;

import java.util.List;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IWorkItemType;

import rtc.pa.model.Project;
import rtc.pa.model.TaskType;
import rtc.pa.utils.ProgressMonitor;

public class WorkItemTypeHelper {

	static String readWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p) {
	
		monitor.out("Now reading work item types...");
		List<IWorkItemType> allWorkItemTypes;
		try {
			allWorkItemTypes = wiClient.findWorkItemTypes(pa, monitor);
			for (IWorkItemType t : allWorkItemTypes) {
				p.putTaskType(new TaskType(t.getIdentifier(), t.getDisplayName()));
				monitor.out("\t" + t.getDisplayName() + " (" + t.getIdentifier() + ')');
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "problem while getting work item types";
		}
		return null;
	}

}
