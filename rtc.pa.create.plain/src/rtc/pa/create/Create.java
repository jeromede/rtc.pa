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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.process.client.IProcessClientService;
import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProcessArea;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.IItemManager;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
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

		String url, proj, user, password, pm, integ, valid, tm1, tm2, cat;
		Date date;
		int current, weeks, number;
		try {
			url = new String(args[0]);
			proj = new String(args[1]);
			user = new String(args[2]);
			password = new String(args[3]);
			pm = new String(args[4]);
			integ = new String(args[5]);
			valid = new String(args[6]);
			tm1 = new String(args[7]);
			tm2 = new String(args[8]);
			date = (new SimpleDateFormat("yyyy-MM-dd")).parse(args[9]);
			number = new Integer(args[10]).intValue();
			weeks = new Integer(args[11]).intValue();
			current = new Integer(args[12]).intValue();
			cat = new String(args[13]);
		} catch (Exception e) {
			monitor.out(
					"arguments: ccm_url project_area_name jazz_admin_id password projectmanager_ID integrator_ID validator_ID teammember1_ID teammember2_ID start_date number_of_iterations number_of_weeks_in_an_iteration current_iteration category_name");
			monitor.out(
					"example: http://rtc.my.rational.com/ccm \"Training 3\" rational nopassword paula ian victoria alice bernard 2020-01-31 6 2 4 \"Simulator Service\"");
			monitor.out("bad args:");
			for (String arg : args) {
				monitor.out(' ' + arg);
			}
			monitor.out();
			return;
		}
		monitor.out("RTC server URL: " + url);
		monitor.out("Project: " + proj);
		monitor.out("JazzAdmin user ID: " + user);
		monitor.out("Password: " + "***");
		monitor.out("Project Manager ID: " + pm);
		monitor.out("Integrator ID: " + integ);
		monitor.out("Validator ID: " + valid);
		monitor.out("Team Member 1 ID: " + tm1);
		monitor.out("Team Member 2 ID: " + tm2);
		monitor.out("Iteration start date" + date);
		monitor.out("Number of iterations: " + number);
		monitor.out("Number of weeks in an iteration: " + weeks);
		monitor.out("Current iteration number: " + current);
		monitor.out("Category name: " + cat);

		TeamPlatform.startup();
		try {
			ITeamRepository repo = Login.login(url, user, password, monitor);
			URI uri = URI.create(proj);
			monitor.out("URI: "+ uri.toASCIIString());
			IProcessClientService processClient = (IProcessClientService) repo
					.getClientLibrary(IProcessClientService.class);
			IProcessArea pa0 = (IProcessArea) (processClient.findProcessArea(uri, IProcessItemService.ALL_PROPERTIES,
					monitor));
			IProjectArea pa = null;
			if (null != pa0 && pa0 instanceof IProjectArea) {
				pa = (IProjectArea) pa0;
				message = execute("", repo, pa, monitor);
			} else {
				message = uri + " is not a project area";
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

	static String execute(String indent, ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor)
			throws TeamRepositoryException, IOException {

		return null;
	}

}
