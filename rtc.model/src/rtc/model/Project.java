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

package rtc.model;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Project extends Item implements Serializable {

	private static final long serialVersionUID = 6293925306051459254L;
	private TextSaver saver = new TextSaver();
	private String name;
	private String uri;
	private String summary;
	private Map<Integer, Member> members = new HashMap<Integer, Member>();
	private Map<String, Member> members0 = new HashMap<String, Member>();
	private Map<Integer, Administrator> administrators = new HashMap<Integer, Administrator>();
	private Map<String, Administrator> administrators0 = new HashMap<String, Administrator>();
	private Map<Integer, Category> categories = new HashMap<Integer, Category>();
	private Map<String, Category> categories0 = new HashMap<String, Category>();
	private Map<Integer, Line> lines = new HashMap<Integer, Line>();
	private Map<String, Line> lines0 = new HashMap<String, Line>();
	private Map<Integer, Iteration> iterations = new HashMap<Integer, Iteration>();
	private Map<String, Iteration> iterations0 = new HashMap<String, Iteration>();
	private Map<Integer, Task> tasks = new HashMap<Integer, Task>();
	private Map<String, Task> tasks0 = new HashMap<String, Task>();
	private Map<Integer, TaskType> taskTypes = new HashMap<Integer, TaskType>();
	private Map<String, TaskType> taskTypes0 = new HashMap<String, TaskType>();

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("name", name)//
				+ Item.SEP + Item.trace("uri", uri)//
				+ Item.SEP + Item.trace("summary", summary);
	}

	public Project(//
			String sourceUUID, //
			String name, //
			String uri, //
			String summary) {
		super(sourceUUID);
		this.name = name;
		this.uri = uri;
		this.summary = summary;
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

	public void putMember(Member member) {
		members.put(member.getUID(), member);
		members0.put(member.getSourceUUID(), member);
	}

	public Member getMember(int uid) {
		return members.get(uid);
	}

	public Member getMember(String userId) {
		return members0.get(userId);
	}

	public Collection<Member> getMembers() {
		return members.values();
	}

	public void putAdministrator(Administrator user) {
		administrators.put(user.getUID(), user);
		administrators0.put(user.getSourceUUID(), user);
	}

	public Administrator getAdministrator(int userId) {
		return administrators.get(userId);
	}

	public Administrator getAdministrator(String sourceUUID) {
		return administrators0.get(sourceUUID);
	}

	public Collection<Administrator> getAdministrators() {
		return administrators.values();
	}

	public void putCategory(Category category) {
		categories.put(category.getUID(), category);
		categories0.put(category.getSourceUUID(), category);
	}

	public Category getCategory(int uid) {
		return categories.get(uid);
	}

	public Category getCategory(String categoryId) {
		return categories0.get(categoryId);
	}

	public Collection<Category> getCategories() {
		return categories.values();
	}

	public void putLine(Line line) {
		lines.put(line.getUID(), line);
		lines0.put(line.getSourceUUID(), line);
	}

	public Line getLine(int uid) {
		return lines.get(uid);
	}

	public Line getLine(String sourceUUID) {
		return lines0.get(sourceUUID);
	}

	public Collection<Line> getLines() {
		return lines.values();
	}

	public void putIteration(Line line, Iteration iteration) {
		line.putIteration(iteration);
		iterations.put(iteration.getUID(), iteration);
		iterations0.put(iteration.getSourceUUID(), iteration);
	}

	public void putIteration(Line line, Iteration parent, Iteration iteration) {
		parent.putIteration(iteration);
		iterations.put(iteration.getUID(), iteration);
		iterations0.put(iteration.getSourceUUID(), iteration);
		parent.putIteration(iteration);
	}

	public Iteration getIteration(int uid) {
		return iterations.get(uid);
	}

	public Iteration getIteration(String sourceUUID) {
		return iterations0.get(sourceUUID);
	}

	public void putTask(Task task) {
		tasks.put(task.getUID(), task);
		tasks0.put(task.getSourceUUID(), task);
	}

	public Task getTask(int uid) {
		return tasks.get(uid);
	}

	public Task getTask(String sourceUUID) {
		return tasks0.get(sourceUUID);
	}

	public Collection<Task> getTasks() {
		return tasks.values();
	}

	public void putTaskType(TaskType taskType) {
		taskTypes.put(taskType.getUID(), taskType);
		taskTypes0.put(taskType.getSourceUUID(), taskType);
	}

	public TaskType getTaskType(int uid) {
		return taskTypes.get(uid);
	}

	public TaskType getTaskType(String id) {
		return taskTypes0.get(id);
	}

	public Collection<TaskType> getTaskTypes() {
		return taskTypes.values();
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
		dump_administrators(out, this.administrators);
		out.println(SEPARATOR + "PROJECT MEMBERS: ");
		dump_users(out, this.members);
		out.println(SEPARATOR + "PROJECT CATEGORIES: ");
		dump_categories(out, this.categories);
		out.println(SEPARATOR + "PROJECT DEVELOPMENT LINES: ");
		dump_lines(out, this.lines);
		out.println(SEPARATOR + "PROJECT TASK TYPES: ");
		dump_taskTypes(out, this.taskTypes);
		out.println(SEPARATOR + "PROJECT TASKS: ");
		dump_tasks(out, this.tasks);
	}

	private static void dump_users(PrintStream out, Map<Integer, Member> users) {
		for (Member user : users.values()) {
			out.println("\n" + user.toString());
		}
	}

	private static void dump_administrators(PrintStream out, Map<Integer, Administrator> users) {
		for (Administrator user : users.values()) {
			out.println("\n" + user.toString());
		}
	}

	private static void dump_categories(PrintStream out, Map<Integer, Category> categories) {
		for (Category category : categories.values()) {
			out.println("\n" + category.toString());
		}
	}

	private static void dump_lines(PrintStream out, Map<Integer, Line> lines) {
		for (Line line : lines.values()) {
			out.println("\n" + line.toString());
		}
	}

	private static void dump_taskTypes(PrintStream out, Map<Integer, TaskType> taskTypes) {
		for (TaskType taskType : taskTypes.values()) {
			out.println("\n" + taskType.toString());
		}
	}

	private static void dump_tasks(PrintStream out, Map<Integer, Task> tasks) {
		for (Task task : tasks.values()) {
			out.println("\n" + task.toString());
		}
	}

}
