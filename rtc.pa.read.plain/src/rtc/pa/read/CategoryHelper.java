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

package rtc.pa.read;

import java.util.List;

import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.CategoryId;
import com.ibm.team.workitem.common.model.ICategory;

import rtc.pa.model.Category;
import rtc.pa.model.Project;
import rtc.pa.utils.ProgressMonitor;

public class CategoryHelper {

	static String readCategories(ITeamRepository repo, IProjectArea pa, IWorkItemClient wiClient,
			IWorkItemCommon wiCommon, ProgressMonitor monitor, Project p) {

		List<ICategory> allCategories;
		//
		// Categories
		//
		monitor.out("Now reading categories...");
		CategoryId parentId;
		try {
			allCategories = wiClient.findCategories(pa, ICategory.FULL_PROFILE, monitor);
			for (ICategory c : allCategories) {
				if (c.isArchived()) {
					continue;
				}
				parentId = c.getParentId2();
				p.putCategory(//
						new Category(//
								c.getCategoryId().getInternalRepresentation(), //
								c.getName(), //
								wiCommon.resolveHierarchicalName(c, monitor), //
								c.getHTMLDescription().getXMLText(), //
								((null == parentId) ? null : parentId.getInternalRepresentation())));
				monitor.out("\tjust added category " + c.getName());
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error reading categories";
		}
		monitor.out("... categories read.");
		return null;
	}

}
