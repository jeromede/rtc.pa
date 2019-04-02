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

import java.io.PrintStream;

import rtc.pa.model.Approval;
import rtc.pa.model.Artifact;
import rtc.pa.model.Attachment;
import rtc.pa.model.Category;
import rtc.pa.model.Comment;
import rtc.pa.model.Iteration;
import rtc.pa.model.Line;
import rtc.pa.model.Link;
import rtc.pa.model.Literal;
import rtc.pa.model.Member;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskVersion;
import rtc.pa.model.Value;

public abstract class Trace2 {

	private static PrintStream out;

	private static void trace(String m) {
		out.println(m);
	}

	private static void trace(String m, String s) {
		out.println(m + ":\t" + s);
	}

	private static void trace(String m, String s1, String s2, String s3) {
		out.println(m + ":\t" + s1 + "\t| " + s2 + "\t| " + s3);
	}

	private static void trace(String m, String s1, String s2) {
		out.println(m + ":\t" + s1 + "\t--> " + s2);
	}

	@SuppressWarnings("unused")
	private static void trace(String m, String s, Integer i) {
		out.println(m + ":\t" + s + "\t--> " + i);
	}

	private static void trace(String m, Integer i) {
		out.println(m + ":\t" + i);
	}

	public static void main(String[] args) {

		Project p = null;
		String ser;
		try {
			ser = new String(args[0]);
		} catch (Exception e) {
			System.err.println("arguments: file");
			System.err.println("example: pa123.ser");
			System.err.print("Bad arguments:");
			for (String arg : args) {
				System.err.println(arg);
			}
			System.err.println();
			return;
		}
		p = Project.deserialize(ser);
		out = System.out;
		dump(p);
	}

	private static void dump(Project p) {
		dump_categories(p);
		dump_timelines(p);
		dump_tasks(p);
	}

	private static void dump_categories(Project p) {
		for (Category c : p.getCategories()) {
			trace("\ncategory", c.getHierarchicalName());
		}
	}

	private static void dump_timelines(Project p) {
		for (Line l : p.getLines()) {
			trace("\nline", l.getId());
			for (Iteration i : l.getIterations()) {
				dump_iteration(i, "\t");
			}
		}
	}

	private static void dump_iteration(Iteration i, String prefix) {
		trace(prefix + "iteration", i.getId());
		for (Iteration j : i.getIterations()) {
			dump_iteration(j, prefix + '\t');
		}
	}

	private static void dump_tasks(Project p) {
		for (Task t : p.getTasks()) {
			trace("\ntask", t.getId());
			dump_task(t);
		}
	}

	private static void dump_task(Task t) {
		trace("\tcreator", id(t.getCreator()));
		trace("\tcreation", t.getCreation().toInstant().toString());
		for (Link l : t.getLinks()) {
			trace("\tlink", l.getType(), (null == l.getTarget()) ? "" : "" + l.getTarget().getId());
		}
		for (Artifact a : t.getArtifacts()) {
			trace("\tartifact", "", a.getURI().toASCIIString());
		}
		for (Attachment a : t.getAttachments()) {
			trace("\tattachment", a.getContentType(), a.getName());
		}
		for (Approval a : t.getApprovals()) {
			trace("\tapproval", a.getType(), a.getState(), id(a.getApprover()));
		}
		for (TaskVersion v : t.getHistory()) {
			trace("\n\tversion of " + t.getId());
			trace("\t\ttype", v.getType().getName());
			trace("\t\tsummary", v.getSummary());
			trace("\t\tstate", state(v));
			trace("\t\tcategory", (null == v.getCategory()) ? "" : v.getCategory().getHierarchicalName());
			trace("\t\ttarget", (null == v.getTarget()) ? "" : v.getTarget().getId());
			trace("\t\towned by", (null == v.getOwnedBy()) ? "" : v.getOwnedBy().getUserId());
			trace("\t\tresolution", v.getResolution2());
			trace("\t\tresolution date", (null == v.getResolution()) ? "" : v.getResolution().toInstant().toString());
			trace("\t\tresolved by", v.getResolvedBy().getUserId());
			trace("\t\tmodified", v.getModified().toInstant().toString());
			trace("\t\tmodified", v.getModified().toInstant().toString());
			trace("\t\tmodified by", (null == v.getModifier()) ? "" : v.getModifier().getUserId());
			trace("\t\tpriority", v.getPriority());
			trace("\t\tseverity", v.getSeverity());
			trace("\t\tdue date", (null == v.getDue()) ? "" : v.getDue().toInstant().toString());
			trace("\t\tduration", "" + v.getDuration());
			trace("\t\tdescription", v.getDescription());
			trace("\t\ttags", v.getTags().toString());
			trace("\t\tsubscribers");
			for (Member m : v.getSubscribers()) {
				trace("\t\t\t" + ((null == m) ? "" : m.getUserId()));
			}
			trace("\t\tother values");
			for (Value val : v.getValues()) {
				if (val.getAttribute().isEnum()) {
					trace("\t\t\t" + val.getAttribute().getName() + ": "
							+ ((Literal) val.getValue()).getSourceId() + "::" + val.getAttribute().getType() + " ("
							+ ((Literal) val.getValue()).getName() + ")");
				} else {
					trace("\t\t\t" + val.getAttribute().getName() + ": " + val.getAttribute().getSourceId() + "::"
							+ val.getAttribute().getType() + " " + val.getValue().toString());
				}
			}
			trace("\t\tcomments");
			for (Comment c : v.getComments()) {
				trace("\t\t\t(by " + ((null == c.getCreator()) ? "" : c.getCreator().getUserId()) + ", "
						+ c.getCreation().toInstant().toString() + "):\n\t\t\t\t\"" + c.getContent() + "\"");
			}
		}
	}

	private static String id(Member m) {
		return (null == m) ? null : m.getUserId();
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

}
