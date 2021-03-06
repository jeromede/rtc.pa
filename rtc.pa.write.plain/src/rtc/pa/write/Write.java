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

package rtc.pa.write;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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
import com.ibm.team.workitem.common.model.IWorkItem;

import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.utils.Login;
import rtc.pa.utils.ProgressMonitor;

public class Write {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		Project p = null;
		Map<String, String> matchingUserIDs = new HashMap<String, String>();
		String message;

		String url, proj, user, password, ser, dir, match, numbers, whenother;
		boolean complete;
		try {
			url = new String(args[0]);
			proj = new String(args[1]);
			user = new String(args[2]);
			password = new String(args[3]);
			complete = new Boolean(args[4]).booleanValue();
			ser = new String(args[5]);
			dir = new String(args[6]);
			match = new String(args[7]);
			numbers = new String(args[8]);
			try {
				whenother = new String(args[9]);
			} catch (Exception e) {
				whenother = "";
			}
		} catch (Exception e) {
			monitor.err(
					"arguments: ccm_url pa user password create_work_items serialization_input_file attachments_input_dir members_input_file wi_ids_match_output_file");
			monitor.err(
					"example: https://my.clm.example.com/ccm \"UU | PPP\" jazz_admin iloveyou true UU_PP.ser attachments_here members.txt ids.txt");
			monitor.err(
					"note: members_input_file has to be a UTF-8 text file with a line for each member; this line should read like:\n\tID_in_source ID_in_target");
			monitor.err(
					"note: create_work_items not true means that only a new version is created for work items found with the same id");
			monitor.err("bad args:");
			for (String arg : args) {
				monitor.err(' ' + arg);
			}
			monitor.err();
			return;
		}
		monitor.out("Target server URL: " + url);
		monitor.out("Target project: " + proj);
		monitor.out("URL: " + user);
		monitor.out("Password: " + "***");
		monitor.out("Serialized input file: " + ser);
		monitor.out("Attachment input dir: " + dir);
		monitor.out("Matching user IDs input file: " + match);
		monitor.out("Matching work item IDs output file: " + numbers);
		monitor.out("Default user ID: " + whenother);

		message = matchingMembers(matchingUserIDs, match, whenother);
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
			URI uri = URI.create(proj.replaceAll(" ", "%20").replaceAll("\\|", "%7C")); // replace necessary?
			IProcessClientService processClient = (IProcessClientService) repo
					.getClientLibrary(IProcessClientService.class);
			IProcessArea pa0 = (IProcessArea) (processClient.findProcessArea(uri, IProcessItemService.ALL_PROPERTIES,
					monitor));
			IProjectArea pa = null;
			if (null != pa0 && pa0 instanceof IProjectArea) {
				pa = (IProjectArea) pa0;
				message = WriteIt.execute(repo, pa, complete, monitor, p, matchingUserIDs, dir, whenother);
			} else {
				message = uri + " is not a project area";
			}
			if (null == message) {
				monitor.out("OK, done.");
				message = writeIdMatchFile(numbers, p);
				if (null != message)
					monitor.err(message);
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

	static String matchingMembers(Map<String, String> map, String filename, String whenother) {
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
			if (0 == whenother.length()) {
				e.printStackTrace();
				return "error reading UTF-8 text file " + filename;
			}
		}
		return null;
	}

	private static String writeIdMatchFile(String filename, Project p) {
		Collection<String> lines = new ArrayList<String>(p.getTasks().size());
		for (Task task : p.getTasks()) {
			lines.add(task.getId() + "\t" + ((IWorkItem) task.getExternalObject()).getId());
		}
		try {
			Files.write(Paths.get(filename), lines, StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
			return "error creating UTF-8 text file " + filename;
		}
		return null;
	}

}
