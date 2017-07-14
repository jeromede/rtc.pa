package rtc.pa.write;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.ICategory;

import rtc.pa.model.Category;
import rtc.pa.model.Project;
import rtc.pa.utils.ProgressMonitor;

public class CategoryHelper {
	static String createCategory(IProjectArea pa, IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p,
			Category cat) {

		ICategory category;
		if (null != cat.getExternalObject()) {
			category = (ICategory) cat.getExternalObject();
			monitor.out("Category already exists " + category.getCategoryId().getInternalRepresentation());
			return null;
		}
		if (null == cat.getParentId()) {
			try {
				category = wiCommon.createCategory(pa, cat.getName(), monitor);
				cat.setExternalObject(category.getCategoryId().getInternalRepresentation(), category);
				monitor.out("\tjust created category \"" + category.getCategoryId().getInternalRepresentation() + "\"");
				finaliseCategory(wiCommon, monitor, cat);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error creating target category for source category \"" + cat.getSourceId() + "\"";
			}
		} else {
			Category par = p.getCategory(cat.getParentId());
			String message = createCategory(pa, wiCommon, monitor, p, par);
			if (null != message) {
				return message;
			}
			try {
				category = wiCommon.createSubcategory((ICategory) par.getExternalObject(), cat.getName(), monitor);
				cat.setExternalObject(category.getCategoryId().getInternalRepresentation(), category);
				monitor.out(
						"Just created subcategory \"" + category.getCategoryId().getInternalRepresentation() + "\"");
				finaliseCategory(wiCommon, monitor, cat);
			} catch (TeamRepositoryException e) {
				e.printStackTrace();
				return "error creating target subcategory for source category \"" + cat.getSourceId() + "\"";
			}
		}
		return null;
	}

	private static void finaliseCategory(IWorkItemCommon wiCommon, ProgressMonitor monitor, Category cat)
			throws TeamRepositoryException {
		ICategory category = (ICategory) cat.getExternalObject();
		category.setHTMLDescription(XMLString.createFromXMLText(cat.getDescription()));
		wiCommon.saveCategory(category, monitor);
	}

}
