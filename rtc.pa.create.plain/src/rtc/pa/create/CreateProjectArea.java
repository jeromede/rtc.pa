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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.team.process.client.IClientProcess;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.client.workingcopies.IProcessAreaWorkingCopy;
import com.ibm.team.process.common.IDescription;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProcessDefinition;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IRole;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;

import rtc.pa.utils.Login;
import rtc.pa.utils.ProgressMonitor;

public class CreateProjectArea {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		String message;

		String url, proj, proc, summ, user, password;
		String[] role;
		String[] tm;
		int a = 0;
		URI uri;
		try {
			url = URI.create(args[a++]).toASCIIString();
			proj = new String(args[a++]);
			uri = URI.create(URLEncoder.encode(args[1], StandardCharsets.US_ASCII.toString()).replaceAll("\\+", "%20"));
			proc = new String(args[a++]);
			summ = new String(args[a++]);
			user = new String(args[a++]);
			password = new String(args[a++]);
			int m = (args.length - a) / 2;
			role = new String[m];
			tm = new String[m];
			int i = 0;
			while (a < args.length) {
				tm[i] = new String(args[a++]);
				role[i++] = new String(args[a++]);
			}
		} catch (Exception e) {
			monitor.out("arguments: ccm_url project_area_name process_ID project_summary" + " jazz_project_admin_id password"
					+ " teammember1_ID teammember1_role" + " ...");
			monitor.out(
					"example: http://rtc.my.rational.com/ccm \"Training 3\" process4.example.com \"Summary of project here\""
							+ " rational nopassword" + " paula \"Software Project Manager\""
							+ " victoria \"Software Validator\"" + " ian \"Software Integrator\""
							+ " ian \"Team Member\"" + " alice \"Team Member\"" + " bernard \"Team Member\"");
			monitor.out("bad args:");
			for (String arg : args) {
				monitor.out(' ' + arg);
			}
			monitor.out();
			return;
		}
		monitor.out("RTC server URL: " + url);
		monitor.out("Project name: " + proj + " (" + uri.toASCIIString() + ")");
		monitor.out("Process ID: " + proc);
		monitor.out("Project description: " + summ);
		monitor.out("JazzProjectAdmin user ID: " + user);
		monitor.out("Password: " + "***");
		for (int i = 0; i < role.length; i++) {
			monitor.out("Member role " + i + ": " + role[i]);
			monitor.out("Member ID " + i + ": " + tm[i]);
		}

		TeamPlatform.startup();
		try {
			ITeamRepository repo = Login.login(url, user, password, monitor);
			IProcessClientService processClient = (IProcessClientService) repo
					.getClientLibrary(IProcessClientService.class);
			IProcessArea pa0 = (IProcessArea) (processClient.findProcessArea(uri, IProcessItemService.ALL_PROPERTIES,
					monitor));
			IProjectArea pa;
			if (null == pa0) {
				monitor.out("About to create PA: " + proj);
				pa = createPa(repo, proj, summ, proc, monitor);
			} else if (pa0 instanceof IProjectArea) {
				monitor.out("PA already exists, found: " + proj);
				pa = (IProjectArea) pa0;
			} else {
				pa = null;
			}
			if (null != pa) {
				message = execute(pa, user, role, tm, monitor);
			} else {
				message = uri.toASCIIString() + " is not a project area and its creation failed";
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

	private static IProjectArea createPa(ITeamRepository repo, String proj, String summ, String proc,
			ProgressMonitor monitor) throws TeamRepositoryException {

		monitor.out(proj + " is not a project area, let's create it...");
		IProjectArea pa = null;
		IProcessItemService processItem = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
		IProcessDefinition definition = processItem.findProcessDefinition(proc, null, monitor);
		if (null == definition) {
			throw new TeamRepositoryException("Process template " + proc + " does not exist.");
		}
		pa = processItem.createProjectArea();
		pa.setName(proj);
		pa.setProcessDefinition(definition);
		IDescription description = pa.getDescription();
		description.setSummary(summ);
		pa = (IProjectArea) processItem.save(pa, monitor);
		processItem.initialize(pa, monitor);
		return pa;
	}

	private static String execute(IProjectArea pa, String user, String[] role, String[] tm, ProgressMonitor monitor)
			throws TeamRepositoryException, IOException {

		addAdministrator(pa, monitor);
		Map<String, List<IRole>> members = new HashMap<String, List<IRole>>();
		List<IRole> roles;
		for (int i = 0; i < tm.length; i++) {
			if (members.containsKey(tm[i])) {
				roles = members.get(tm[i]);
			} else {
				roles = new LinkedList<IRole>();
				members.put(tm[i], roles);
			}
			roles.add(getRoleFromID(role[i], pa, monitor));
		}
		IRole[] r;
		for (String id : members.keySet()) {
			r = new IRole[members.get(id).size()];
			r = (IRole[]) members.get(id).toArray(r);
			addMember(pa, id, r, monitor);
		}
		return null;
	}

	private static IContributor addAdministrator(IProjectArea pa, ProgressMonitor monitor)
			throws TeamRepositoryException {

		ITeamRepository repo = (ITeamRepository) pa.getOrigin();
		monitor.out("About to add administrator...");
		IContributor admin = repo.loggedInContributor();
		IProcessItemService processItem = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
		IProcessAreaWorkingCopy paWc = (IProcessAreaWorkingCopy) processItem.getWorkingCopyManager()
				.createPrivateWorkingCopy(pa);
		paWc.getAdministrators().addContributors(new IContributor[] { admin });
		paWc.save(monitor);
		monitor.out("... administrator added.");
		return admin;
	}

	private static IContributor addMember(IProjectArea pa, String userId, IRole[] roles, ProgressMonitor monitor)
			throws TeamRepositoryException {

		ITeamRepository repo = (ITeamRepository) pa.getOrigin();
		monitor.out("About to add/replace member " + userId + " (ID) with roles...");
		for (IRole role : roles) {
			monitor.out("\t" + role.getId());
		}
		IContributor member = null;
		try {
			member = repo.contributorManager().fetchContributorByUserId(userId, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return null;
		}
		if (null == member) {
			return null;
		}
		IProcessItemService processItem = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
		IProcessAreaWorkingCopy paWc = (IProcessAreaWorkingCopy) processItem.getWorkingCopyManager()
				.createPrivateWorkingCopy(pa);
		paWc.getTeam().addContributorsSettingRoleCast(new IContributor[] { member }, roles);
		paWc.save(monitor);
		monitor.out("... member added/replaced.");
		return member;
	}

	@SuppressWarnings("unused")
	private static IContributor removeMember(IProjectArea pa, String userId, ProgressMonitor monitor)
			throws TeamRepositoryException {

		ITeamRepository repo = (ITeamRepository) pa.getOrigin();
		monitor.out("About to remove member " + userId + " ...");
		IContributor member = null;
		try {
			member = repo.contributorManager().fetchContributorByUserId(userId, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return null;
		}
		if (null == member) {
			return null;
		}
		IProcessItemService processItem = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
		IProcessAreaWorkingCopy paWc = (IProcessAreaWorkingCopy) processItem.getWorkingCopyManager()
				.createPrivateWorkingCopy(pa);
		paWc.getTeam().removeContributors(new IContributorHandle[] { member });
		paWc.save(monitor);
		monitor.out("... member removed.");
		return member;
	}

	private static IRole getRoleFromID(String roleId, IProcessArea pa, ProgressMonitor monitor)
			throws TeamRepositoryException {

		ITeamRepository repo = (ITeamRepository) pa.getOrigin();
		IProcessItemService processItem = (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
		IClientProcess clientProcess = processItem.getClientProcess(pa, monitor);
		IRole[] availableRoles = clientProcess.getRoles(pa, monitor);
		for (int i = 0; i < availableRoles.length; i++) {
			IRole role = availableRoles[i];
			if (role.getId().equalsIgnoreCase(roleId))
				return role;
		}
		throw new TeamRepositoryException("Role " + roleId + " does not exist.");
	}


}