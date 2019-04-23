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

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.ICategory;

import rtc.pa.utils.Login;
import rtc.pa.utils.ProgressMonitor;

public class AddCategory {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		String message;

		String url, proj, user, password, cat;
		int a = 0;
		URI uri;
		try {
			url = URI.create(args[a++]).toASCIIString();
			proj = new String(args[a++]);
			uri = URI.create(URLEncoder.encode(args[1], StandardCharsets.US_ASCII.toString()).replaceAll("\\+", "%20"));
			user = new String(args[a++]);
			password = new String(args[a++]);
			cat = new String(args[a++]);
		} catch (Exception e) {
			monitor.out("arguments: ccm_url project_area_name" + " user_id password" + " category_name");
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
				message = execute(pa, cat, monitor);
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

	private static String execute(IProjectArea pa, String cat, ProgressMonitor monitor)
			throws TeamRepositoryException, IOException {

		if (null == addCategory(pa, cat, monitor)) {
			return "Could not add category " + cat;
		}
		return null;
	}

	private static ICategory addCategory(IProjectArea pa, String cat, ProgressMonitor monitor)
			throws TeamRepositoryException {

		ITeamRepository repo = (ITeamRepository) pa.getOrigin();
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		ICategory category;
		category = wiCommon.createCategory(pa, cat, monitor);
		wiCommon.saveCategory(category, monitor);
		return category;
	}

}