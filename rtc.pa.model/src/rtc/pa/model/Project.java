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

package rtc.pa.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import rtc.pa.text.TextSaver;

public class Project extends Item implements Serializable {

	private static final long serialVersionUID = 6293925306051459254L;
	private TextSaver saver = new TextSaver();
	private String name;
	private String process;
	private String uri;
	private String summary;
	private Map<String, Member> members = new HashMap<String, Member>();
	private Map<String, Administrator> administrators = new HashMap<String, Administrator>();
	private Map<String, Category> categories = new HashMap<String, Category>();
	private Map<String, Line> lines = new HashMap<String, Line>();
	private Map<String, Iteration> iterations = new HashMap<String, Iteration>();
	private Map<String, Task> tasks = new HashMap<String, Task>();
	private Map<String, TaskType> taskTypes = new HashMap<String, TaskType>();
	private Map<String, Attribute> attributes = new HashMap<String, Attribute>();
	private SortedMap<Date, TaskVersion> history = new TreeMap<Date, TaskVersion>();

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("name", name)//
				+ Item.SEP + Item.trace("process", process)//
				+ Item.SEP + Item.trace("uri", uri)//
				+ Item.SEP + Item.trace("summary", summary);
	}

	public Project(//
			String sourceId, //
			String name, //
			String process, //
			String uri, //
			String summary) {
		super(sourceId);
		this.name = name;
		this.process = process;
		this.uri = uri;
		this.summary = summary;
	}

	public void resolve() {
		for (Task task : tasks.values()) {
			for (Link link : task.getLinks()) {
				System.out.println("resolve link (type: " //
						+ link.getType() //
						+ ") " //
						+ link.getTargetId() //
						+ " from " + task.getId() //
				);
				link.resolve(this.getTask(link.getTargetId()));
				if (null == link.getTarget()) {
					System.out.println("\tto unknown target work item (" + link.getTargetId() + ')');
				} else {
					System.out.println("\tto " + link.getTarget().getId());
				}
			}
		}
	}

	public TextSaver saver() {
		return this.saver;
	}

	public String getName() {
		return this.name;
	}

	public String getUri() {
		return this.uri;
	}

	public String getSummary() {
		return this.summary;
	}

	public void putMember(Member user) {
		members.put(user.getSourceId(), user);
	}

	public Member getMember(String userId) {
		return members.get(userId);
	}

	public Collection<Member> getMembers() {
		return members.values();
	}

	public void putAdministrator(Administrator user) {
		administrators.put(user.getSourceId(), user);
	}

	public Administrator getAdministrator(String sourceId) {
		return administrators.get(sourceId);
	}

	public void putCategory(Category category) {
		categories.put(category.getSourceId(), category);
	}

	public Category getCategory(String categoryId) {
		return categories.get(categoryId);
	}

	public Collection<Category> getCategories() {
		return categories.values();
	}

	public void putLine(Line line) {
		lines.put(line.getSourceId(), line);
	}

	public Line getLine(String sourceId) {
		return lines.get(sourceId);
	}

	public Collection<Line> getLines() {
		return lines.values();
	}

	public void putIteration(Line line, Iteration iteration) {
		line.putIteration(iteration);
		iterations.put(iteration.getSourceId(), iteration);
	}

	public void putIteration(Line line, Iteration parent, Iteration iteration) {
		parent.putIteration(iteration);
		iterations.put(iteration.getSourceId(), iteration);
		parent.putIteration(iteration);
	}

	public Iteration getIteration(String sourceId) {
		return iterations.get(sourceId);
	}

	public void putTask(Task task) {
		tasks.put(task.getSourceId(), task);
	}

	public Task getTask(String sourceId) {
		return tasks.get(sourceId);
	}

	public Collection<Task> getTasks() {
		return tasks.values();
	}

	public void putTaskType(TaskType taskType) {
		taskTypes.put(taskType.getSourceId(), taskType);
	}

	public TaskType getTaskType(String id) {
		return taskTypes.get(id);
	}

	public Collection<TaskType> getTaskTypes() {
		return taskTypes.values();
	}

	public void putAttribute(TaskType taskType, Attribute attribute) {
		taskType.putAttribute(attribute);
		attributes.put(attribute.getSourceId(), attribute);
	}

	public Attribute getAttribute(String id) {
		return attributes.get(id);
	}

	public Collection<Attribute> getAttributes() {
		return attributes.values();
	}

	public void putTaskVersion(TaskVersion version) {
		version.getTask().putTaskVersion(version);
		history.put(version.getModified(), version);
	}

	public Collection<TaskVersion> getHistory() {
		return history.values();
	}

	public void serialize(String filename) {
		try {
			FileOutputStream fileOut = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in " + filename);
		} catch (IOException i) {
			i.printStackTrace();
		}
	}

	public static Project deserialize(String filename) {
		try {
			Project p;
			FileInputStream fileIn = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			p = (Project) in.readObject();
			in.close();
			fileIn.close();
			return p;
		} catch (IOException i) {
			i.printStackTrace();
		} catch (ClassNotFoundException c) {
			System.out.println("Project class not found");
			c.printStackTrace();
		}
		return null;
	}

	private static final String SEPARATOR = "\n-----------------\n";

	public void dump(PrintStream out) {
		out.println("PROJECT: " + this.toString());
		out.println(SEPARATOR + "PROJECT ADMINS: ");
		dump_administrators(out, this.administrators.values());
		out.println(SEPARATOR + "PROJECT MEMBERS: ");
		dump_users(out, this.members.values());
		out.println(SEPARATOR + "PROJECT CATEGORIES: ");
		dump_categories(out, this.categories.values());
		out.println(SEPARATOR + "PROJECT DEVELOPMENT LINES: ");
		dump_lines(out, this.lines.values());
		out.println(SEPARATOR + "PROJECT TASK TYPES: ");
		dump_taskTypes(out, this.taskTypes.values());
		out.println(SEPARATOR + "PROJECT TASKS: ");
		dump_tasks(out, this.tasks.values());
	}

	private static void dump_users(PrintStream out, Collection<Member> users) {
		for (Member user : users) {
			out.println("\n" + user.toString());
		}
	}

	private static void dump_administrators(PrintStream out, Collection<Administrator> users) {
		for (Administrator user : users) {
			out.println("\n" + user.toString());
		}
	}

	private static void dump_categories(PrintStream out, Collection<Category> categories) {
		for (Category category : categories) {
			out.println("\n" + category.toString());
		}
	}

	private static void dump_lines(PrintStream out, Collection<Line> lines) {
		for (Line line : lines) {
			out.println("\n" + line.toString());
		}
	}

	private static void dump_taskTypes(PrintStream out, Collection<TaskType> taskTypes) {
		for (TaskType taskType : taskTypes) {
			out.println("\n" + taskType.toString());
		}
	}

	private static void dump_tasks(PrintStream out, Collection<Task> tasks) {
		for (Task task : tasks) {
			out.println("\n" + task.toString());
		}
	}

}
