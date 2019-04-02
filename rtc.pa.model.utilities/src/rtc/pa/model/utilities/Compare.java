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

package rtc.pa.model.utilities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtc.pa.model.Link;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskVersion;

public abstract class Compare {

	@SuppressWarnings("unused")
	private static void trace() {
		System.out.println();
	}

	private static void trace(String s) {
		System.out.println(s);
	}

	@SuppressWarnings("unused")
	private static void trace(String t, String s) {
		System.out.println(t + ": " + s);
	}

	private static void trace(String t, int i) {
		System.out.println(t + ": " + i);
	}

	private static void trace(String t, int i, int j) {
		System.out.println(t + ": " + i + " | " + j);
	}

	private static void trace(String t, String s1, String s2) {
		System.out.println(t + ": \"" + s1 + "\" | \"" + s2 + '\"');
	}

	@SuppressWarnings("serial")
	private static final Collection<String> linkTypes = Collections.unmodifiableList(new ArrayList<String>() {
		{
			add("com.ibm.team.workitem.linktype.blocksworkitem");
			add("com.ibm.team.workitem.linktype.copiedworkitem");
			add("com.ibm.team.workitem.linktype.duplicateworkitem");
			add("com.ibm.team.workitem.linktype.parentworkitem");
		}
	});

	private static void matchingIds(Map<Integer, Integer> map, String filename) throws IOException {
		List<String> lines;
		String l;
		String n1, n2;
		lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
		int i;
		for (String line : lines) {
			l = line.trim();
			if (!l.isEmpty()) {
				i = l.indexOf('\t');
				n1 = l.substring(0, i);
				n2 = l.substring(i, l.length()).trim();
				map.put(new Integer(n1), new Integer(n2));
			}
		}
	}

	public static void main(String[] args) throws IOException {

		String ser1, ser2, ids;
		try {
			ser1 = new String(args[0]);
			ser2 = new String(args[1]);
			ids = new String(args[2]);
		} catch (Exception e) {
			System.err.println("arguments: source.ser target.ser match_ids");
			System.err.print("Wrong arguments:");
			for (String arg : args) {
				System.err.println(arg);
			}
			System.err.println();
			return;
		}
		Project p1 = Project.deserialize(ser1);
		Project p2 = Project.deserialize(ser2);
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		matchingIds(map, ids);
		trace("About to compare projects \n\t1) <" + p1.getUri() + "web/projects/" + p1.getName() + ">\n\t2) <"
				+ p2.getUri() + "web/projects/" + p2.getName() + ">");
		trace("\tCriteria:");
		trace("\t- same number of work items");
		trace("\t- same number of links between two corresponding work items");
		trace("\t  (only the following types of links are considered:");
		for (String type : linkTypes) {
			trace("\t  - " + type);
		}
		if (same(p1, p2, map)) {
			trace("... looks like the same projects :-)");
		} else {
			trace("... different projects :-(");
		}
	}

	private static boolean same(Project p1, Project p2, Map<Integer, Integer> map) {
		boolean result = true;
		if (p1.getTasks().size() == p2.getTasks().size()) {
			trace("OK so far, same total number of work items", p1.getTasks().size());
		} else {
			trace("KO, different total number of work items", p1.getTasks().size(), p2.getTasks().size());
			return false;
		}
		Task t1;
		Task t2;
		Integer i2;
		for (Integer i1 : map.keySet()) {
			i2 = map.get(i1);
			t1 = getTask(p1, i1);
			if (null == t1) {
				trace("\tsomething is wrong, project 1 doesn't contain work item", i1);
			}
			t2 = getTask(p2, i2);
			if (null == t2) {
				trace("\tsomething is wrong, project 2 doesn't contain work item", i1);
			}
			trace("Comparison for work item", t1.getId(), t2.getId());
			if (same(t1, t2, map)) {
				trace("OK so far, same work items", t1.getId(), t2.getId());
			} else {
				trace("KO, different work items", t1.getId(), t2.getId());
				trace("\t... continue anyway...");
				result = false;
			}
		}
		return result;
	}

	private static boolean same(Task t1, Task t2, Map<Integer, Integer> map) {
		boolean result = true;
		trace("\tState comparison for work item", t1.getId(), t2.getId());
		if (!state(last(t1)).equals(state(last(t2)))) {
			trace("\tKO, different final states", state(last(t1)), state(last(t2)));
			trace("\t... continue anyway...");
		}
		trace("\tLink comparison for work item", t1.getId(), t2.getId());
		int n1 = trace_links(t1);
		int n2 = trace_links(t2);
		if (n1 != n2) {
			trace("\tKO, different total number of links between the two", n1, n2);
			trace("\t... continue anyway...");
			result = false;
		}
		Link l2;
		for (Link l1 : t1.getLinks()) {
			if (!linkTypes.contains(l1.getType())) {
				continue;
			}
			l2 = isThere(l1, t2, map);
			if (null == l2) {
				trace("\tKO, didn't find in " + t2.getId() + " a link equivalent to " + t1.getId() + "--("
						+ l1.getType() + ")-->");
				trace("\t... continue anyway...");
				result = false;
			} else {
				trace("\tOK here,    " + t1.getId() + "--(" + l1.getType() + ")-->" + l1.getTarget().getId()//
						+ "\n\tequivalent to " + t2.getId() + "--(" + l2.getType() + ")-->" + l2.getTarget().getId()//
				);
			}
		}
		return result;
	}

	private static Link isThere(Link l1, Task t2, Map<Integer, Integer> map) {
		Task other1 = l1.getTarget();
		Task other2;
		for (Link l2 : t2.getLinks()) {
			if (!linkTypes.contains(l2.getType())) {
				continue;
			}
			other2 = l2.getTarget();
			if (l1.getType().equals(l2.getType()) && (map.get(other1.getId()) == other2.getId())) {
				return l2;
			}
		}
		return null;
	}

	private static Task getTask(Project p, int id) {
		for (Task t : p.getTasks()) {
			if (t.getId() == id) {
				return t;
			}
		}
		return null;
	}

	private static TaskVersion last(Task task) {
		TaskVersion result = null;
		for (TaskVersion v : task.getHistory()) {
			result = v;
		}
		return result;
	}

	private static String state(TaskVersion version) {
		String state = version.getState();
		String type = version.getType().getSourceId();
		if (1 == state.length()) {
			if (type.equals("defect")) {
				return "com.ibm.team.workitem.defectWorkflow.state.s" + state;
			} else if (type.equals("task")) {
				return "com.ibm.team.workitem.taskWorkflow.state.s" + state;
			}
		}
		return state;
	}

	private static int trace_links(Task t) {
		// trace("\t\tLinks for work item", t.getId());
		int n = 0;
		for (Link l : t.getLinks()) {
			if (null != l.getTarget()) {
				if (linkTypes.contains(l.getType())) {
					// trace("\t\t\t---(" + l.getType() + ")---> " +
					// l.getTarget().getId());
					n++;
				}
			}
		}
		return n;
	}

}
