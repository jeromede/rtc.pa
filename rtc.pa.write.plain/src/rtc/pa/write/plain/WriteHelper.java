package rtc.pa.write.plain;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.ICategory;

import rtc.model.Category;
import rtc.model.Project;
import rtc.utils.ProgressMonitor;

public class WriteHelper {

	public static ICategory createCategoryIfNotExist(IProjectArea pa, IWorkItemCommon wiCommon, ProgressMonitor monitor,
			Project p, Category cat) {
		ICategory category = null;
		if (null != cat.getTargetObject()) {
			category = (ICategory) cat.getTargetObject();
			monitor.out("Category already exists " + category.getName());
			return category;
		}
		if (null == cat.getParentId()) {
			try {
				category = wiCommon.createCategory(pa, cat.getName(), monitor);
				cat.setTargetObject(category.getCategoryId().getInternalRepresentation(), category);
				monitor.out("Created category " + cat.getName());
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			ICategory parent = createCategoryIfNotExist(pa, wiCommon, monitor, p, p.getCategory(cat.getParentId()));
			if (null == parent) {
				return null;
			}
			try {
				category = wiCommon.createSubcategory(parent, cat.getName(), monitor);
				cat.setTargetObject(category.getCategoryId().getInternalRepresentation(), category);
				monitor.out("Created subcategory " + cat.getName() + " of parent " + parent.getName());
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return null;
			}
		}
		return category;
	}

	public static ICategory finaliseCategory(Category cat) {
		ICategory category = (ICategory) cat.getTargetObject();
		category.setName(cat.getName());
		return category;
	}

}
