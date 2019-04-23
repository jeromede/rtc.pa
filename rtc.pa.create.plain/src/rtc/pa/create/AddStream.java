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
import com.ibm.team.scm.client.IWorkspaceConnection;
import com.ibm.team.scm.client.IWorkspaceManager;
import com.ibm.team.scm.client.SCMPlatform;

import rtc.pa.utils.Login;
import rtc.pa.utils.ProgressMonitor;

public class AddStream {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		String message;

		String url, proj, user, password, name, description;
		int a = 0;
		URI uri;
		try {
			url = URI.create(args[a++]).toASCIIString();
			proj = new String(args[a++]);
			uri = URI.create(URLEncoder.encode(args[1], StandardCharsets.US_ASCII.toString()).replaceAll("\\+", "%20"));
			user = new String(args[a++]);
			password = new String(args[a++]);
			name = new String(args[a++]);
			description = new String(args[a++]);
		} catch (Exception e) {
			monitor.out(
					"arguments: ccm_url project_area_name" + " user_id password" + " stream_name stream_description");
			monitor.out("example: http://rtc.my.rational.com/ccm \"Training 3\"" + " ian nopassword"
					+ " \"Integration\" \"Note: only for integrators\"");
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
		monitor.out("Stream name: " + name);
		monitor.out("Stream description: " + description);

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
				message = execute(pa, name, description, monitor);
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

	private static String execute(IProjectArea pa, String name, String description, ProgressMonitor monitor)
			throws TeamRepositoryException, IOException {

		if (null == addStream(pa, name, description, monitor)) {
			return "Could not add stream " + name;
		}
		return null;
	}

	private static IWorkspaceConnection addStream(IProjectArea pa, String name, String description, ProgressMonitor monitor)
			throws TeamRepositoryException {

		ITeamRepository repo = (ITeamRepository) pa.getOrigin();
		IWorkspaceManager wm = SCMPlatform.getWorkspaceManager(repo);
		IWorkspaceConnection stream = wm.createStream((IProcessArea) pa, name, description, monitor);
		return stream;
	}

}