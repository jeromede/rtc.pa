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

package rtc.pa.change;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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

public class Main {

	static Map<String, String> att_enumlist_map;
	static Map<String, String> att_enum_map;
	static Map<String, String> att_other_map;
	static Map<String, String> enumlist_map;
	static Map<String, String> enum_map;
	static Map<String, String> literal_map;
	static Map<String, String> resolution_map;
	static Map<String, String> state_map;
	static Map<String, String> type_map;

	public static void main(String[] args) {

		ProgressMonitor monitor = new ProgressMonitor();
		String message;

		String url, proj, user, password, tsv_dir;
		try {
			url = new String(args[0]);
			proj = new String(args[1]);
			user = new String(args[2]);
			password = new String(args[3]);
			tsv_dir = new String(args[4]);
		} catch (Exception e) {
			monitor.out("arguments: ccm_url pa user password tsv_dir");
			monitor.out(
					"example: https://my.clm.example.com/ccm \"UU | PPP\" jazz_admin iloveyou state_map.tsv /home/rtc/params/");
			monitor.out("bad args:");
			for (String arg : args) {
				monitor.out(' ' + arg);
			}
			monitor.out();
			return;
		}
		monitor.out("Target server URL: " + url);
		monitor.out("Target project: " + proj);
		monitor.out("URL: " + user);
		monitor.out("User ID: " + user);
		monitor.out("Password: " + "***");
		monitor.out("TSV files dir: " + tsv_dir);

		att_enumlist_map = new HashMap<String, String>();
		message = readMap(att_enumlist_map, tsv_dir + "att_enumlist_map.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
			return;
		}
		att_enum_map = new HashMap<String, String>();
		message = readMap(att_enum_map, tsv_dir + "att_enum_map.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
			return;
		}
		att_other_map = new HashMap<String, String>();
		message = readMap(att_other_map, tsv_dir + "att_other_map.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
			return;
		}
		enumlist_map = new HashMap<String, String>();
		message = readMap(enumlist_map, tsv_dir + "enumlist_map.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
			return;
		}
		enum_map = new HashMap<String, String>();
		message = readMap(enum_map, tsv_dir + "enum_map.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
			return;
		}
		literal_map = new HashMap<String, String>();
		message = readMap(literal_map, tsv_dir + "literal_map.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
			return;
		}
		resolution_map = new HashMap<String, String>();
		message = readMap(resolution_map, tsv_dir + "resolution.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
			return;
		}
		state_map = new HashMap<String, String>();
		message = readMap(state_map, tsv_dir + "state_map.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
			return;
		}
		type_map = new HashMap<String, String>();
		message = readMap(type_map, tsv_dir + "type_map.tsv");
		if (null != message) {
			monitor.out("KO: " + message);
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
				message = execute("", repo, pa, monitor, state_map, literal_map);
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

	static String execute(String indent, ITeamRepository repo, IProjectArea pa, ProgressMonitor monitor,
			Map<String, String> state_map, Map<String, String> literal_map)
			throws TeamRepositoryException, IOException {

		IWorkItemClient wiClient = (IWorkItemClient) repo.getClientLibrary(IWorkItemClient.class);
		IWorkItemCommon wiCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		IWorkItemWorkingCopyManager wiCopier = wiClient.getWorkItemWorkingCopyManager();
		IItemManager itemManager = repo.itemManager();

		String result;
		monitor.out(indent + "Now reading/changing work items...");
		IAuditableClient auditableClient = (IAuditableClient) repo.getClientLibrary(IAuditableClient.class);
		IQueryClient queryClient = (IQueryClient) repo.getClientLibrary(IQueryClient.class);
		IQueryableAttribute attribute = null;
		try {
			attribute = QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE).findAttribute(pa,
					IWorkItem.PROJECT_AREA_PROPERTY, auditableClient, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error finding work item attributes";
		}
		Expression expression = new AttributeExpression(attribute, AttributeOperation.EQUALS, pa);
		IQueryResult<IResolvedResult<IWorkItem>> results = queryClient.getResolvedExpressionResults(pa, expression,
				IWorkItem.FULL_PROFILE);
		results.setLimit(Integer.MAX_VALUE);
		try {
			while (results.hasNext(monitor)) {
				result = changeWorkItem("\t" + indent, results.next(monitor).getItem(), repo, pa, wiClient, wiCommon,
						wiCopier, itemManager, monitor);
				if (null != result)
					return result;
			}
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error while reading set of work items";
		}
		monitor.out(indent + "... work items read/changed.");

		return null;
	}

	static String changeWorkItem(String indent, IWorkItem wi, ITeamRepository repo, IProjectArea pa,
			IWorkItemClient wiClient, IWorkItemCommon wiCommon, IWorkItemWorkingCopyManager wiCopier,
			IItemManager itemManager, ProgressMonitor monitor) {

		String previousStateIdentifier = wi.getState2().getStringIdentifier();
		Identifier<IResolution> resolution2Id = wi.getResolution2();
		String previousResolutionIdentifier = null;
		if (null != resolution2Id) {
			previousResolutionIdentifier = resolution2Id.getStringIdentifier();
		}
		monitor.out("\n" + indent + "Now changing work item " + wi.getId() + " (current type: " + wi.getWorkItemType()
				+ ", current state: " + previousStateIdentifier + ", current resolution: "
				+ previousResolutionIdentifier + ")");
		IWorkItemHandle wiH;
		WorkItemWorkingCopy wc;
		IWorkItem wic;
		try {
			wiH = (IWorkItemHandle) wi.getItemHandle();
			wiCopier.connect(wiH, IWorkItem.FULL_PROFILE, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "impossible to initialize an update for an existing work item (" + wi.getId() + ")";
		}
		wc = wiCopier.getWorkingCopy(wiH);
		wic = wc.getWorkItem();
		IDetailedStatus detailedStatus;
		//
		// Change type
		//
		try {
			changeType(indent, pa, wiCommon, wic, monitor);
		} catch (TeamRepositoryException e1) {
			e1.printStackTrace();
			return "impossible to update work item type for work item (" + wi.getId() + ")";
		}
		monitor.out(indent + "... work item " + wic.getId() + ", type: " + wic.getWorkItemType() + ", state: "
				+ wic.getState2().getStringIdentifier());
		//
		// Save
		//
		//detailedStatus = wc.save(monitor);
		//if (!detailedStatus.isOK()) {
		//	return "error\n\t" + detailedStatus.getMessage() + "\nwhile changing version for work item " + wic.getId()
		//			+ " after changing its type";
		//}
		//monitor.out(indent + "... work item " + wic.getId() + " version saved after type change, now type: "
		//		+ wic.getWorkItemType() + ", state: " + wic.getState2().getStringIdentifier());
		//
		// Change state
		//
		try {
			changeState("\t" + indent, pa, wiCommon, wic, monitor, previousStateIdentifier,
					previousResolutionIdentifier);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			monitor.out(indent + "Error: couldn't change wi " + wic.getId() + " state");
		}
		monitor.out(indent + "... work item " + wic.getId() + " type: " + wic.getWorkItemType() + ", state: "
				+ wic.getState2().getStringIdentifier() + ", resolution: " + wic.getResolution2());
		//
		// Save
		//
		//detailedStatus = wc.save(monitor);
		//if (!detailedStatus.isOK()) {
		//	return "error\n\t" + detailedStatus.getMessage() + "\nwhile changing version for work item " + wic.getId()
		//			+ " after changing its state";
		//}
		monitor.out(indent + "... work item " + wic.getId() + " version saved after state change, now type: "
				+ wic.getWorkItemType() + ", state: " + wic.getState2().getStringIdentifier());
		//
		// Change custom attributes
		//
		List<IAttributeHandle> customAttributeHandles = wic.getCustomAttributes();
		Map<String, IAttribute> customAttributes = new HashMap<String, IAttribute>();
		IFetchResult custom = null;
		try {
			custom = itemManager.fetchCompleteItemsPermissionAware(customAttributeHandles, IItemManager.DEFAULT,
					monitor);
		} catch (TeamRepositoryException e3) {
			e3.printStackTrace();
			return "impossible to get custom attributes";
		}
		IAttribute attribute;
		for (Object o : custom.getRetrievedItems()) {
			try {
				attribute = (IAttribute) o;
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			customAttributes.put(attribute.getIdentifier(), attribute);
		}
		try {
			changeAttributes("\t" + indent, repo, pa, wiCommon, wic, customAttributes, monitor);
		} catch (TeamRepositoryException e) {
			e.printStackTrace();
			return "error while changing custom attributes";
		}
		//
		// Save
		//
		detailedStatus = wc.save(monitor);
		if (!detailedStatus.isOK()) {
			return "error\n\t" + detailedStatus.getMessage() + "\nwhile changing version for work item " + wic.getId()
					+ " after changing its attributes";
		}
		monitor.out(indent + "... new work item version saved");

		return null;
	}

	static void changeType(String indent, IProjectArea pa, IWorkItemCommon wiCommon, IWorkItem wi,
			ProgressMonitor monitor) throws TeamRepositoryException {

		String newTypeId = type_map.get(wi.getWorkItemType());
		if (null == newTypeId) {
			monitor.out(indent + "Can't find new work item type identifier matching " + wi.getWorkItemType()
					+ ", skip change");
			return;
		}
		IWorkItemType previousType = wiCommon.findWorkItemType2(pa, wi.getWorkItemType(), monitor);
		IWorkItemType type = previousType;
		type = wiCommon.findWorkItemType2(pa, newTypeId, monitor);
		if (null == type) {
			monitor.out(indent + "Can't find new work item type for " + newTypeId + ", skip change");
			return;
		}
		monitor.out(indent + "Found new work item type " + type.getDisplayName() + " " + type.getIdentifier());
		wiCommon.updateWorkItemType(wi, type, previousType, monitor);
		return;
	}

	static void changeState(String indent, IProjectArea pa, IWorkItemCommon wiCommon, IWorkItem wi,
			ProgressMonitor monitor, String previousStateIdentifier, String previousResolutionIdentifier)
			throws TeamRepositoryException {

		String newStateIdentifier = state_map.get(previousStateIdentifier);
		if (null == newStateIdentifier) {
			monitor.out(
					indent + "can't find new state for previous state " + previousStateIdentifier + ", skip change");
			return;
		}
		forceState("\t" + indent, pa, wiCommon, wi, monitor, newStateIdentifier);
		monitor.out(indent + "(in copy) state changed from: " + previousStateIdentifier + " to: "
				+ wi.getState2().getStringIdentifier());
		String newResolutionIdentifier = resolution_map.get(previousResolutionIdentifier);
		if (null != newResolutionIdentifier) {
			if (newResolutionIdentifier.isEmpty()) {
				newResolutionIdentifier = null;
			}
		}
		Identifier<IResolution> resolution2Id = null;
		if (null != newResolutionIdentifier) {
			resolution2Id = Identifier.create(IResolution.class, newResolutionIdentifier);
		}
		wi.setResolution2(resolution2Id);
		monitor.out(indent + "(in copy) resolution changed from: " + previousStateIdentifier + " to: "
				+ newResolutionIdentifier);
	}

	static void changeAttributes(String indent, ITeamRepository repo, IProjectArea pa, IWorkItemCommon wiCommon,
			IWorkItem wi, Map<String, IAttribute> attributes, ProgressMonitor monitor) throws TeamRepositoryException {

		String newIndent = "\t" + indent;
		IAttribute newAttribute;
		String newAttributeIdentifier;
		String newEnumIdentifier;
		String newLiteralIdentifier;
		Identifier<? extends ILiteral> newLiteralId;
		for (IAttribute attribute : attributes.values()) {
			monitor.out(indent + "now, change attribute: " + attribute.getIdentifier() + " : "
					+ attribute.getAttributeType());

			if (AttributeTypes.isEnumerationAttributeType(attribute.getAttributeType())) {
				monitor.out(newIndent + "is enumeration attribute type");
				newAttributeIdentifier = att_enum_map.get(attribute.getIdentifier());
				if (null == newAttributeIdentifier) {
					monitor.out(newIndent + "no matching identifier for attribute " + attribute.getIdentifier());
					continue;
				}
				monitor.out(newIndent + "matching identifier found for attribute " + attribute.getIdentifier() + ": "
						+ newAttributeIdentifier);
				newAttribute = attributes.get(newAttributeIdentifier);
				if (null == newAttribute) {
					monitor.out(newIndent + "no matching attribute for " + newAttributeIdentifier);
					continue;
				}
				monitor.out(newIndent + "matching attribute found for " + newAttributeIdentifier);
				monitor.out(newIndent + "now, change attribute from " + attribute.getIdentifier() + ":"
						+ attribute.getAttributeType() + " to " + newAttribute.getIdentifier() + ":"
						+ newAttribute.getAttributeType());
				newEnumIdentifier = enum_map.get(attribute.getAttributeType());
				if (null == newEnumIdentifier) {
					monitor.out(newIndent + "no matching enum for " + attribute.getAttributeType());
					continue;
				}
				monitor.out(newIndent + "now, change enum from " + attribute.getAttributeType() + " to "
						+ newEnumIdentifier);
				@SuppressWarnings("unchecked")
				Identifier<? extends ILiteral> literalId = (Identifier<? extends ILiteral>) wi.getValue(attribute);
				newLiteralIdentifier = literal_map.get(literalId.getStringIdentifier());
				if (null == newLiteralIdentifier) {
					monitor.out(newIndent + "no matching literal for (id) " + literalId.getStringIdentifier());
					continue;
				}
				monitor.out(newIndent + "now, change litteral from (ids) " + literalId.getStringIdentifier() + " to "
						+ newLiteralIdentifier);
				newLiteralId = getLiteralId(repo, wiCommon, monitor, newAttribute, newLiteralIdentifier);
				if (null == newLiteralId) {
					monitor.out(newIndent + "no literal found in repository for (id) " + newLiteralIdentifier);
					continue;
				}
				monitor.out(newIndent + "now, set litteral value from " + literalId.getStringIdentifier() + " to "
						+ newLiteralIdentifier);
				wi.setValue(newAttribute, newLiteralId);
				monitor.out(newIndent + "new value set");
			} else if (AttributeTypes.isEnumerationListAttributeType(attribute.getAttributeType())) {
				monitor.out(newIndent + "is enumeration list attribute type");
				newAttributeIdentifier = att_enumlist_map.get(attribute.getIdentifier());
				if (null == newAttributeIdentifier) {
					monitor.out(newIndent + "no matching identifier for attribute " + attribute.getIdentifier());
					continue;
				}
				newAttribute = attributes.get(newAttributeIdentifier);
				if (null == newAttribute) {
					monitor.out(newIndent + "no matching attribute for " + newAttributeIdentifier);
					continue;
				}
				monitor.out(newIndent + "now, change attribute from " + attribute.getIdentifier() + ":"
						+ attribute.getAttributeType() + " to " + newAttribute.getIdentifier() + ":"
						+ newAttribute.getAttributeType());
				@SuppressWarnings("unchecked")
				List<Identifier<? extends ILiteral>> literalIds = (ArrayList<Identifier<? extends ILiteral>>) wi
						.getValue(attribute);
				List<Identifier<? extends ILiteral>> newLiteralIds = new ArrayList<Identifier<? extends ILiteral>>();
				for (Identifier<? extends ILiteral> literalId : literalIds) {
					newLiteralIdentifier = literal_map.get(literalId.getStringIdentifier());
					if (null == newLiteralIdentifier) {
						monitor.out(newIndent + "no matching literal for (id) " + literalId.getStringIdentifier());
						continue;
					}
					newLiteralId = getLiteralId(repo, wiCommon, monitor, newAttribute, newLiteralIdentifier);
					if (null == newLiteralId) {
						monitor.out(newIndent + "no literal found in repository for (id) " + newLiteralIdentifier);
						continue;
					}
					newLiteralIds.add(newLiteralId);
				}
				monitor.out(newIndent + "now, change litteral list from " + wi.getValue(attribute) + " to "
						+ newLiteralIds);
				wi.setValue(newAttribute, newLiteralIds);
				monitor.out(newIndent + "new value set");
			} else {
				monitor.out("\t" + indent + "is other attribute type");
				newAttributeIdentifier = att_other_map.get(attribute.getIdentifier());
				if (null == newAttributeIdentifier) {
					monitor.out(newIndent + "no matching identifier for attribute " + attribute.getIdentifier());
					continue;
				}
				newAttribute = attributes.get(newAttributeIdentifier);
				if (null == newAttribute) {
					monitor.out(newIndent + "no matching attribute for " + newAttributeIdentifier);
					continue;
				}
				monitor.out(newIndent + "now, change attribute value from " + attribute.getIdentifier() + ":"
						+ attribute.getAttributeType() + " to " + newAttribute.getIdentifier() + ":"
						+ newAttribute.getAttributeType());
				wi.setValue(newAttribute, wi.getValue(attribute));
				monitor.out(newIndent + "new value set");
			}
		}
	}

	static Identifier<? extends ILiteral> getLiteralId(ITeamRepository repo, IWorkItemCommon wiCommon,
			ProgressMonitor monitor, //
			IAttribute attribute, String literalIdentifier) throws TeamRepositoryException {

		IEnumeration<? extends ILiteral> enumeration = wiCommon.resolveEnumeration(attribute, monitor);
		ILiteral literal;
		for (Object o : enumeration.getEnumerationLiterals()) {
			literal = (ILiteral) o;
			if (0 == literal.getIdentifier2().getStringIdentifier().compareTo(literalIdentifier)) {
				return literal.getIdentifier2();
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	static void forceState(String indent, IProjectArea pa, IWorkItemCommon wiCommon, IWorkItem wi,
			ProgressMonitor monitor, String state) throws TeamRepositoryException {

		Identifier<IState> target = null;
		IWorkflowInfo wf = wiCommon.getWorkflow(wi.getWorkItemType(), pa, monitor);
		Identifier<IState>[] states = wf.getAllStateIds();
		for (Identifier<IState> s : states) {
			if (s.getStringIdentifier().equals(state)) {
				target = s;
				break;
			}
		}
		if (null == target) {
			monitor.out(indent + "Problem: didn't find state " + state + " for wi " + wi.getId() + " of type "
					+ wi.getWorkItemType() + ", nothing to change");
			return;
		}
		wi.setState2(target);
		monitor.out(indent + "wi " + wi.getId() + " forced to state " + wi.getState2().getStringIdentifier());
		return;
	}

	static String readMap(Map<String, String> map, String filename) {
		try {
			List<String> lines;
			String l;
			lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
			int i;
			for (String line : lines) {
				l = line.trim();
				if (!l.isEmpty()) {
					i = l.indexOf('\t');
					if (-1 == i) {
						map.put(l, null);
					} else {
						map.put(l.substring(0, i), l.substring(i, l.length()).trim());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "error reading UTF-8 text file " + filename;
		}
		return null;
	}

}
