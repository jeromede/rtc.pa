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

package rtc.pa.write;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;

import rtc.pa.model.Project;
import rtc.pa.utils.Login;
import rtc.pa.utils.ProgressMonitor;

public class Main {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		Project p = null;
		Map<String, String> matchingUserIDs = new HashMap<String, String>();
		String message;

		String url, proj, user, password;
		String ser, match;
		try {
			url = new String(args[0]);
			proj = new String(args[1]);
			user = new String(args[2]);
			password = new String(args[3]);
			ser = new String(args[4]);
			match = new String(args[5]);
		} catch (Exception e) {
			monitor.err("arguments: url user password serialization_file members_file");
			monitor.err(
					"example: https://my.clm.example.com/ccm \"UU | PPP\" jazz_admin iloveyou UU_PP.ser members.txt");
			monitor.err(
					"note: the last argument has to be a UTF-8 text file with a line for each member; this line should read like:\n\tID_in_source ID_in_target");
			monitor.err("bad args:");
			for (String arg : args) {
				monitor.err(' ' + arg);
			}
			monitor.err();
			return;
		}
		message = matchingMembers(matchingUserIDs, match);
		if (null != message) {
			monitor.err("problem with the matching members file: " + message);
			return;
		}
		p = Project.deserialize(ser);
		if (null == p) {
			monitor.err("problem reading serialized project from " + ser);
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
			if (null != pa0 && pa0 instanceof IProjectArea) {
				pa = (IProjectArea) pa0;
				message = WriteIt.execute(repo, pa, monitor, p, matchingUserIDs);
			} else {
				message = uri + " is not a project area";
			}
			if (null == message) {
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

	static String matchingMembers(Map<String, String> map, String filename) {
		try {
			List<String> lines;
			String l;
			lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
			int i;
			for (String line : lines) {
				l = line.trim();
				if (!l.isEmpty()) {
					i = l.indexOf(' ');
					map.put(l.substring(0, i), l.substring(i, l.length()).trim());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "error reading UTF-8 text file " + filename;
		}
		return null;
	}

}
