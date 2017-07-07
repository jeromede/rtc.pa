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

package rtc.pa.read.plain;

import java.io.IOException;
import java.net.URI;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;

import rtc.model.Project;
import rtc.utils.Login;
import rtc.utils.ProgressMonitor;

public class Main {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		Project p = null;

		String url, proj, user, password;
		String ser;
		try {
			url = new String(args[0]);
			proj = new String(args[1]);
			user = new String(args[2]);
			password = new String(args[3]);
			ser = new String(args[4]);
		} catch (Exception e) {
			monitor.err("arguments: url user password serialization_file");
			monitor.err("example: https://hub.jazz.net/ccm01 \"UU | PPP\" jazz_admin iloveyou UU_PP.ser");
			System.err.print("Bad arguments:");
			for (String arg : args) {
				monitor.err(arg);
			}
			monitor.err();
			return;
		}
		TeamPlatform.startup();
		try {
			ITeamRepository repo = Login.login(url, user, password, monitor);
			URI uri = URI.create(proj.replaceAll(" ", "%20").replaceAll("\\|", "%7C"));
			IProcessClientService processClient = (IProcessClientService) repo
					.getClientLibrary(IProcessClientService.class);
			IProcessArea pa0 = (IProcessArea) (processClient.findProcessArea(uri, IProcessItemService.ALL_PROPERTIES,
					monitor));
			IProjectArea pa = null;
			String message;
			if (null != pa0 && pa0 instanceof IProjectArea) {
				pa = (IProjectArea) pa0;
				p = new Project(pa.getItemId().getUuidValue(), pa.getName(), repo.getRepositoryURI(), pa.getDescription().getSummary());
				message = ReadIt.execute(repo, pa, monitor, p);
			} else {
				message = new String(uri + " is not a project area");
			}
			if (null == message && null != pa && null != p) {
				p.serialize(ser);
				monitor.out("OK, done.");
			} else {
				monitor.err("KO: " + message);
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			monitor.err("Unable to perform: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			monitor.err("IO error: " + e.getMessage());
		} finally {
			TeamPlatform.shutdown();
		}
	}

}
