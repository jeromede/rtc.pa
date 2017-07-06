package rtc.pa.write.plain;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
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

import rtc.model.Project;
import rtc.utils.ProgressMonitor;

public class DoIt {

	public static String execute(ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor, Project p)
			throws TeamRepositoryException, IOException {

		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IWorkItemWorkingCopyManager wiCopier = wiClient.getWorkItemWorkingCopyManager();
		return writeWorkItem(repo, pa, wiClient, wiCommon, wiCopier, monitor, p);
	}

	private static String writeWorkItem(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier, ProgressMonitor monitor, Project p) {

		//
		// Type
		//
		List<IWorkItemType> all;
		IWorkItemType wiType = null;
		try {
			wiType = wiClient.findWorkItemType(pa, "com.ibm.team.apt.workItemType.story", monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while retrieving work item types.");
		}
		if (null == wiType) {
			return ("Error: can't find work item type.");
		}
		//
		// Category
		//
		List<ICategory> findCategories;
		try {
			findCategories = wiClient.findCategories(pa, ICategory.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while retrieving categories.");
		}
		ICategory category = findCategories.get(0);
		//
		// Creation
		//
		IWorkItemHandle wiHandle;
		try {
			wiHandle = wiCopier.connectNew(wiType, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while creating work item.");
		}
		WorkItemWorkingCopy wc = wiCopier.getWorkingCopy(wiHandle);
		IWorkItem wi = wc.getWorkItem();
		IDetailedStatus s;
		try {
			wi.setCategory(category);
			wi.setCreator(repo.loggedInContributor());
			wi.setOwner(repo.loggedInContributor());
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10"));
			Timestamp t = new Timestamp(1098480784979L);
			wi.setCreationDate(t);
			List<IAttributeHandle> customAttributes = wi.getCustomAttributes();
			for (IAttributeHandle a:customAttributes) {
				System.out.println("custom attribute " + a.toString());
			}
			IAttribute tpModified;
			try {
				tpModified = wiClient.findAttribute(pa, "tp_modified", monitor);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return ("Can't find custom attribute tp_modified.");
			}
			System.out.println("custom attribute tp_modified " + tpModified.toString());
			wi.setValue(tpModified, t);
			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("Error saving new work item");
			}
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10 updated"));
			wi.setRequestedModified(t);
			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("Error updating new work item");
			}
		} finally {
			wiCopier.disconnect(wi);
		}
		try {
			wi = (IWorkItem) repo.itemManager().fetchCompleteItem(wi, IItemManager.DEFAULT, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error fetching new work item.");
		}
		int wiN = wi.getId();
		System.out.println("Created workitem: " + wiN);
		//
		// Update
		//
		try {
			wi = wiClient.findWorkItemById(wiN, IWorkItem.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Can't find work item " + wiN);
		}
		try {
			wiCopier.connect(wi, IWorkItem.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return ("Error while connecting to work item.");
		}
		wc = wiCopier.getWorkingCopy(wi);
		wi = wc.getWorkItem();
		try {
			wi.setHTMLSummary(XMLString.createFromPlainText("Example work item 10 modified"));
			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return ("Error saving updated work item");
			}
		} finally {
			wiClient.getWorkItemWorkingCopyManager().disconnect(wi);
		}

		return null;
	}

}
