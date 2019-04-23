/*
 * Copyright (c) 2017,2018,2019 Jérôme Desquilbet <jeromede@fr.ibm.com>
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

package rtc.pa.create;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.ibm.team.foundation.common.text.XMLString;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.model.ICategory;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;

import rtc.pa.utils.Login;
import rtc.pa.utils.ProgressMonitor;

public class AddWorkItem {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		String message;

		String url, proj, user, password, type, summary, cat;
		int a = 0;
		URI uri;
		try {
			url = URI.create(args[a++]).toASCIIString();
			proj = new String(args[a++]);
			uri = URI.create(URLEncoder.encode(args[1], StandardCharsets.US_ASCII.toString()).replaceAll("\\+", "%20"));
			user = new String(args[a++]);
			password = new String(args[a++]);
			type = new String(args[a++]);
			summary = new String(args[a++]);
			cat = new String(args[a++]);
		} catch (Exception e) {
			monitor.out("arguments: ccm_url project_area_name" + " user_id password"
					+ " work_item_type_name work_item_summary category_name");
			monitor.out("example: http://rtc.my.rational.com/ccm \"Training 3\"" + " paula nopassword"
					+ " \"Simulator Service\"");
			monitor.out("bad args:");
			for (String arg : args) {
				monitor.out(' ' + arg);
			}
			monitor.out();
			return;
		}
		monitor.out("RTC server URL: " + url);
		monitor.out("Project name: " + proj + " (" + uri.toASCIIString() + ")");
		monitor.out("User ID: " + user);
		monitor.out("Password: " + "***");
		monitor.out("Category name: " + cat);

		TeamPlatform.startup();
		try {
			ITeamRepository repo = Login.login(url, user, password, monitor);
			IProcessClientService processClient = (IProcessClientService) repo
					.getClientLibrary(IProcessClientService.class);
			IProcessArea pa0 = (IProcessArea) (processClient.findProcessArea(uri, IProcessItemService.ALL_PROPERTIES,
					monitor));
			IProjectArea pa;
			if (null == pa0) {
				pa = null;
			} else if (pa0 instanceof IProjectArea) {
				monitor.out("PA exists, found: " + proj);
				pa = (IProjectArea) pa0;
			} else {
				pa = null;
			}
			if (null != pa) {
				message = execute(pa, type, summary, cat, monitor);
			} else {
				message = uri.toASCIIString() + " is not a project area";
			}
			if (null == message) {
				monitor.out("OK, done.");
			} else {
				monitor.out("KO: " + message);
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			monitor.out("Unable to perform: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			monitor.out("IO error: " + e.getMessage());
		} finally {
			TeamPlatform.shutdown();
		}
	}

	private static String execute(IProjectArea pa, String type, String summary, String cat, ProgressMonitor monitor)
			throws TeamRepositoryException, IOException {

		return addWorkItem(pa, type, summary, cat, monitor);
	}

	private static String addWorkItem(IProjectArea pa, String type, String summary, String cat, ProgressMonitor monitor)
			throws TeamRepositoryException {

		ITeamRepository repo = (ITeamRepository) pa.getOrigin();
		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		List<IWorkItemType> wiTypes = wiClient.findWorkItemTypes(pa, monitor);
		IWorkItemType wiType = null;
		for (IWorkItemType t : wiTypes) {
			if (0 == t.getDisplayName().compareTo(type)) {
				wiType = t;
				break;
			}
		}
		if (null == wiType) {
			return "could not find work item type " + type;
		}
		IWorkItemWorkingCopyManager wiCopier = wiClient.getWorkItemWorkingCopyManager();
		IWorkItemHandle wiH;
		try {
			wiH = wiCopier.connectNew(wiType, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "impossible to initialize a new work item";
		}
		WorkItemWorkingCopy wc = wiCopier.getWorkingCopy(wiH);
		IWorkItem wi = wc.getWorkItem();
		IDetailedStatus s;
		try {
			wi.setCreator(repo.loggedInContributor());
			List<ICategory> categories = wiClient.findCategories(pa, ICategory.FULL_PROFILE, monitor);
			ICategory category = null;
			for (ICategory c : categories) {
				if (0 == c.getName().compareTo(cat)) {
					category = c;
					break;
				}
			}
			if (null == category) {
				return "could not find category " + cat;
			}
			wi.setCategory(category);
			wi.setHTMLSummary(XMLString.createFromPlainText(summary));
			s = wc.save(monitor);
			if (!s.isOK()) {
				s.getException().printStackTrace();
				return "error creating minimal new work item";
			}

		} finally {
			wiCopier.disconnect(wi);
		}
		return null;
	}

}