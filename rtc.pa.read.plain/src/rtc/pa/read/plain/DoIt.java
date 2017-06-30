package rtc.pa.read.plain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.team.process.common.IDevelopmentLine;
import com.ibm.team.process.common.IDevelopmentLineHandle;
import com.ibm.team.process.common.IIteration;
import com.ibm.team.process.common.IIterationHandle;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IContributorManager;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.scm.client.IWorkspaceManager;
import com.ibm.team.scm.client.SCMPlatform;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.Expression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.ICategoryHandle;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;

import rtc.model.Administrator;
import rtc.model.Category;
import rtc.model.Member;
import rtc.model.Project;
import rtc.utils.ProgressMonitor;

public class DoIt {

	public static String execute(ITeamRepository repo, IProjectArea pa, IPath dir, ProgressMonitor monitor, Project p)
			throws TeamRepositoryException, IOException {

		IItemManager itemManager = repo.itemManager();
		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);
		String result;

		result = readMembers(repo, pa, monitor, p);
		if (null != result)
			return result;
		result = readCategories(repo, pa, wiClient, wiCommon, dir, monitor, p);
		if (null != result)
			return result;
		// result = readDevelopmentLines(repo, pa, auditableClient, itemManager,
		// dir, monitor);
		if (null != result)
			return result;
		// result = readWorkItemTypes(repo, pa, wiClient, wiCommon, dir,
		// monitor);
		if (null != result)
			return result;
		// result = readWorkItems(repo, pa, wiClient, wiCommon, itemManager,
		// dir, monitor);
		if (null != result)
			return result;

		return null;
	}

	private static String readContributors(IContributorHandle[] contributors, ITeamRepository repo, IProjectArea pa,
			ProgressMonitor monitor, Project p, boolean isAdmin) {

		IContributor contrib;
		for (IContributorHandle contribHandle : contributors) {
			try {
				contrib = (IContributor) repo.itemManager().fetchCompleteItem(contribHandle, IItemManager.DEFAULT,
						monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "Error resolving IContributorHandle";
			}
			if (isAdmin) {
				p.putAdministrator(new Administrator(contrib.getItemId().getUuidValue(), contrib.getUserId(), contrib.getName()));				
				monitor.out("Added administrator " + contrib.getUserId());
			} else {
				p.putMember(new Member(contrib.getItemId().getUuidValue(), contrib.getUserId(), contrib.getName()));
				monitor.out("Added member " + contrib.getUserId());
			}
		}
		return null;
	}

	private static String readMembers(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p) {

		String result;
		result = readContributors(pa.getAdministrators(), repo, pa, monitor, p, true);
		if (null != result)
			return result;
		result = readContributors(pa.getMembers(), repo, pa, monitor, p, false);
		if (null != result)
			return result;
		return null;
	}

	private static String readCategories(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IPath dir, ProgressMonitor monitor, Project p) {

		com.ibm.team.foundation.common.text.XMLString x;
		List<ICategory> allCategories;
		try {
			allCategories = wiClient.findCategories(pa, ICategory.FULL_PROFILE, monitor);
			for (ICategory c : allCategories) {
				p.putCategory(new Category(c.getItemId().getUuidValue(), c.getName(),
						wiCommon.resolveHierarchicalName(c, monitor), c.getHTMLDescription().getXMLText()));
				monitor.out("Added category " + c.getName());
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error reading categories";
		}
		return null;
	}

	private static String readDevelopmentLines(ITeamRepository repo, IProjectArea pa, IAuditableClient auditableClient,
			IItemManager itemManager, IPath dir, ProgressMonitor monitor) {

		IDevelopmentLineHandle[] devLines = pa.getDevelopmentLines();
		for (IDevelopmentLineHandle devLineHandle : devLines) {
			readDevelopmentLine(devLineHandle, repo, pa, auditableClient, itemManager, dir, monitor);
		}
		return null;
	}

	private static String readDevelopmentLine(IDevelopmentLineHandle devLineHandle, ITeamRepository repo,
			IProjectArea pa, IAuditableClient auditableClient, IItemManager itemManager, IPath dir,
			ProgressMonitor monitor) {

		//
		// Development line
		//
		IDevelopmentLine devLine;
		try {
			devLine = auditableClient.resolveAuditable(devLineHandle, ItemProfile.DEVELOPMENT_LINE_DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Error resolving development line handle.";
		}
		System.out.println("development line " + devLine.toString());

		//
		// Iterations
		//
		for (IIterationHandle iterationHandle : devLine.getIterations()) {
			readIteration(devLine, iterationHandle, repo, pa, auditableClient, itemManager, dir, "\t", monitor);
		}

		return null;
	}

	private static String readIteration(IDevelopmentLine devLine, IIterationHandle iterationHandle,
			ITeamRepository repo, IProjectArea pa, IAuditableClient auditableClient, IItemManager itemManager,
			IPath dir, String prefix, ProgressMonitor monitor) {

		IIteration iteration;
		try {
			iteration = auditableClient.resolveAuditable(iterationHandle, ItemProfile.ITERATION_DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Error resolving iteration handle.";
		}

		System.out.println(prefix + "iteration " + iteration.toString());
		for (IIterationHandle children : iteration.getChildren()) {
			readIteration(devLine, children, repo, pa, auditableClient, itemManager, dir, prefix + "\t", monitor);
		}

		return null;
	}

	private static String readWorkItemTypes(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IPath dir, ProgressMonitor monitor) {

		System.out.println("work item types [");
		List<IWorkItemType> allWorkItemTypes;
		try {
			allWorkItemTypes = wiClient.findWorkItemTypes(pa, monitor);
			for (IWorkItemType t : allWorkItemTypes) {
				System.out.println(t.getIdentifier());
			}
		} catch (TeamRepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("]");
		return null;
	}

	private static String readWorkItems(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, IPath dir, ProgressMonitor monitor) {

		IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);
		IQueryClient queryClient = (IQueryClient) repo.getClientLibrary(IQueryClient.class);
		IQueryableAttribute attribute = null;
		try {
			attribute = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE).findAttribute(pa,
					IWorkItem.PROJECT_AREA_PROPERTY, auditableClient, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Error finding work item attributes.";
		}
		Expression expression = new AttributeExpression(attribute, AttributeOperation.EQUALS, pa);
		IQueryResult<IResolvedResult<IWorkItem>> results = queryClient.getResolvedExpressionResults(pa, expression,
				IWorkItem.FULL_PROFILE);
		results.setLimit(Integer.MAX_VALUE);
		IResolvedResult<IWorkItem> result;
		IWorkItem wi;
		try {
			while (results.hasNext(monitor)) {
				result = results.next(monitor);
				wi = result.getItem();
				readWorkItem(wi, repo, pa, wiClient, wiCommon, itemManager, dir, "", monitor);
			}
		} catch (TeamRepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private static String readWorkItem(IWorkItem wi, ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IItemManager itemManager, IPath dir, String prefix, ProgressMonitor monitor) {

		readWorkItemVersion(wi, repo, pa, wiClient, wiCommon, itemManager, dir, prefix, monitor);

		IAuditableHandle auditableHandle = (IAuditableHandle) wi.getItemHandle();
		List<IWorkItemHandle> handles;
		List<IWorkItem> workItems;
		try {
			handles = itemManager.fetchAllStateHandles(auditableHandle, monitor);
			workItems = itemManager.fetchCompleteStates(handles, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Error reading history of work item " + wi.getId();
		}
		for (IWorkItem w : workItems) {
			readWorkItemVersion(w, repo, pa, wiClient, wiCommon, itemManager, dir, prefix + "\t", monitor);
		}

		return null;
	}

	private static String readWorkItemVersion(IWorkItem wi, ITeamRepository repo, IProjectArea pa,
			IWorkItemClient wiClient, IWorkItemCommon wiCommon, IItemManager itemManager, IPath dir, String prefix,
			ProgressMonitor monitor) {

		System.out.println("\n" + prefix + "workitem " + wi.getId() + " " + wi.getCreationDate() + " — " + wi
				.getFullState().toString().replaceAll(", ", "\n " + prefix).replaceAll(" \\(", "\n" + prefix + "\\("));
		System.out.println(prefix + "TARGET: " + ((null == wi.getTarget()) ? "null" : wi.getTarget().toString()));
		System.out.println(prefix + "MODIFIED: " + wi.modified().toString());
		IContributor contributor;
		try {
			contributor = (IContributor) repo.itemManager().fetchCompleteItem((IContributorHandle) wi.getModifiedBy(),
					IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Problem retrieving contributor for workitem.";
		}
		System.out.println(prefix + "MODIFIER: " + contributor.getUserId() + " (" + contributor.getName() + " <"
				+ contributor.getEmailAddress() + ">)");
		try {
			System.out.println(prefix + "CATEGORY: "
					+ wiCommon.resolveHierarchicalName((ICategoryHandle) wi.getCategory(), monitor));
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "Error while reading category for work item " + wi.getId();
		}
		System.out.println(prefix + "TARGET: " + ((null == wi.getTarget()) ? "null" : wi.getTarget().toString()));

		return null;
	}

}
