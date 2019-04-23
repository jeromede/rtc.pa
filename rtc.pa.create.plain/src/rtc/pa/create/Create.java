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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.team.process.client.IClientProcess;
import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.client.workingcopies.IProcessAreaWorkingCopy;
import com.ibm.team.process.common.IDescription;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProcessDefinition;
import com.ibm.team.process.common.IProcessItem;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IRole;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IFetchResult;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IDetailedStatus;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.client.IWorkItemWorkingCopyManager;
import com.ibm.team.workitem.client.WorkItemWorkingCopy;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.Expression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.QueryableAttributes;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.AttributeTypes;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IAttributeHandle;
import com.ibm.team.workitem.common.model.IEnumeration;
import com.ibm.team.workitem.common.model.ILiteral;
import com.ibm.team.workitem.common.model.IResolution;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemHandle;
import com.ibm.team.workitem.common.model.IWorkItemType;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResolvedResult;
import com.ibm.team.workitem.common.workflow.IWorkflowInfo;

class Login {

	public static ITeamRepository login(String repositoryAddress, final String user, final String password,
			IProgressMonitor monitor) throws TeamRepositoryException {

		ITeamRepository repository = TeamPlatform.getTeamRepositoryService().getTeamRepository(repositoryAddress);
		repository.registerLoginHandler(new ITeamRepository.ILoginHandler() {
			public ILoginInfo challenge(ITeamRepository repository) {
				return new ILoginInfo() {
					public String getUserId() {
						return user;
					}

					public String getPassword() {
						return password;
					}
				};
			}
		});
		monitor.subTask("Contacting " + repository.getRepositoryURI() + "...");
		repository.login(monitor);
		monitor.subTask("Connected");
		return repository;
	}

}

class ProgressMonitor implements IProgressMonitor {

	public void beginTask(String name, int totalWork) {
		out(name);
	}

	public void done() {
	}

	public void internalWorked(double work) {
	}

	public boolean isCanceled() {
		return false;
	}

	public void setCanceled(boolean value) {
	}

	public void setTaskName(String name) {
		out(name);
	}

	public void subTask(String name) {
		out(name);
	}

	public void worked(int work) {
	}

	public void out() {
		System.out.println();
	}

	public void out(String message) {
		if (null != message && !"".equals(message))
			System.out.println(message);
	}

	public void err() {
		System.err.println();
	}

	public void err(String message) {
		if (null != message && !"".equals(message))
			System.err.println(message);
	}

}

public class Create {

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		String message;

		String url, proj, proc, summ, user, password, cat;
		String[] role = new String[5];
		String[] tm = new String[5];
		Date date;
		int current, weeks, number;
		URI uri;
		try {
			url = URI.create(args[0]).toASCIIString();
			proj = new String(args[1]);
			uri = URI.create(URLEncoder.encode(args[1], StandardCharsets.US_ASCII.toString()).replaceAll("\\+", "%20"));
			proc = new String(args[2]);
			summ = new String(args[3]);
			user = new String(args[4]);
			password = new String(args[5]);
			role[0] = new String(args[6]);
			tm[0] = new String(args[7]);
			role[1] = new String(args[8]);
			tm[1] = new String(args[9]);
			role[2] = new String(args[10]);
			tm[2] = new String(args[11]);
			role[3] = new String(args[12]);
			tm[3] = new String(args[13]);
			role[4] = new String(args[14]);
			tm[4] = new String(args[15]);
			date = (new SimpleDateFormat("yyyy-MM-dd")).parse(args[16]);
			number = new Integer(args[17]).intValue();
			weeks = new Integer(args[18]).intValue();
			current = new Integer(args[19]).intValue();
			if (14 == args.length) {
				cat = new String(args[20]);
			} else {
				cat = proj;
			}
		} catch (Exception e) {
			monitor.out("arguments: ccm_url project_area_name process_ID project_summary" + " jazz_admin_id password"
					+ " teammember1_role teammember1_ID" + " teammember2_role teammember2_ID"
					+ " teammember3_role teammember3_ID" + " teammember4_role teammember4_ID"
					+ " teammember5_role teammember5_ID"
					+ " start_date number_of_iterations number_of_weeks_in_an_iteration current_iteration"
					+ " [category_name]");
			monitor.out(
					"example: http://rtc.my.rational.com/ccm \"Training 3\" scrum2.process.ibm.com \"Summary of project here\""
							+ " rational nopassword" + " \"Software Project Manager\" paula"
							+ " \"Software Validator\" victoria" + " \"Software Integrator\" ian"
							+ " \"Team Member\" alice" + " \"Team Member\" bernard" + " 2020-01-31 6 2 4"
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
		monitor.out("Process ID: " + proc);
		monitor.out("Project description: " + summ);
		monitor.out("JazzAdmin user ID: " + user);
		monitor.out("Password: " + "***");
		for (int i = 0; i < 5; i++) {
			monitor.out("Member role " + i + ": " + role[i]);
			monitor.out("Member ID " + i + ": " + tm[i]);
		}
		monitor.out("Iteration start date" + date);
		monitor.out("Number of iterations: " + number);
		monitor.out("Number of weeks in an iteration: " + weeks);
		monitor.out("Current iteration number: " + current);
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
				monitor.out("About to create PA: " + proj);
				pa = createPa(repo, proj, summ, proc, monitor);
			} else if (pa0 instanceof IProjectArea) {
				monitor.out("PA already exists, found: " + proj);
				pa = (IProjectArea) pa0;
			} else {
				pa = null;
			}
			if (null != pa) {
				message = execute(pa, role, tm, date, number, weeks, current, cat, monitor);
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

	private static String execute(IProjectArea pa, String[] role, String[] tm, Date date,
			int number, int weeks, int current, String cat, ProgressMonitor monitor)
			throws TeamRepositoryException, IOException {

		for (int i = 0; i < 5; i++) {
			addMember(pa, tm[i], new IRole[] { getRoleFromID(role[i], pa, monitor) }, monitor);
		}
		return null;
	}

	private static IContributor addMember(IProjectArea pa, String userId, IRole[] roles,
			ProgressMonitor monitor) throws TeamRepositoryException {

		ITeamRepository repo = (ITeamRepository) pa.getOrigin();
		monitor.out("About to add/replace member " + userId + " (ID) with roles ");
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
		monitor.out("... member added/replaced");
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
