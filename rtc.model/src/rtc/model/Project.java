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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Project extends Item implements Serializable {

	private static final long serialVersionUID = 6293925306051459254L;

	private String name;
	private Map<Integer, Member> members = new HashMap<Integer, Member>();
	private Map<String, Member> members0 = new HashMap<String, Member>();
	private Map<Integer, Administrator> administrators = new HashMap<Integer, Administrator>();
	private Map<String, Administrator> administrators0 = new HashMap<String, Administrator>();
	private Map<Integer, Category> categories = new HashMap<Integer, Category>();
	private Map<String, Category> categories0 = new HashMap<String, Category>();
	private Map<Integer, Line> lines = new HashMap<Integer, Line>();
	private Map<String, Line> lines0 = new HashMap<String, Line>();
	private Map<Integer, Task> tasks = new HashMap<Integer, Task>();
	private Map<String, Task> tasks0 = new HashMap<String, Task>();

	public String toString() {
		return super.toString() + Item.SEP + Item.trace("name", name);
	}

	public Project(String name) {
		this(null, name);
	}

	public Project(String oldId, String name) {
		super(oldId);
		this.name = new String(name);
	}

	public String getName() {
		return this.name;
	}

	public void putMember(Member member) {
		members.put(member.getId(), member);
		members0.put(member.getOldId(), member);
	}

	public Member getMember(int id) {
		return members.get(id);
	}

	public Member getMember(String id) {
		return members0.get(id);
	}

	public void putAdministrator(Administrator user) {
		administrators.put(user.getId(), user);
		administrators0.put(user.getOldId(), user);
	}

	public Administrator getAdministrator(int id) {
		return administrators.get(id);
	}

	public Administrator getAdministrator(String id) {
		return administrators0.get(id);
	}

	public void putCategory(Category category) {
		categories.put(category.getId(), category);
		categories0.put(category.getOldId(), category);
	}

	public Category getCategory(int id) {
		return categories.get(id);
	}

	public Category getCategory(String id) {
		return categories0.get(id);
	}

	public void putLine(Line line) {
		lines.put(line.getId(), line);
		lines0.put(line.getOldId(), line);
	}

	public Line getLine(int id) {
		return lines.get(id);
	}

	public Line getLine(String id) {
		return lines0.get(id);
	}

	public void putTask(Task task) {
		tasks.put(task.getId(), task);
		tasks0.put(task.getOldId(), task);
	}

	public Task getTask(int id) {
		return tasks.get(id);
	}

	public Task getTask(String id) {
		return tasks0.get(id);
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
		Project p = null;
		try {
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

	public String dump() {
		return "project: " + this.toString() + "\nproject admins: " + dump_administrators(this.administrators)
				+ "\nproject members: " + dump_users(this.members) + "\nproject categories: "
				+ dump_categories(this.categories) + "\nproject development lines: " + dump_lines(this.lines)
				+ "\nproject tasks: " + dump_tasks(this.tasks);
	}

	private static String dump_users(Map<Integer, Member> users) {
		String buffer = new String();
		Member user;
		for (Integer i : users.keySet()) {
			user = users.get(i);
			buffer = buffer + "\n\t" + user.toString();
		}
		return buffer;
	}

	private static String dump_administrators(Map<Integer, Administrator> users) {
		String buffer = new String();
		Administrator user;
		for (Integer i : users.keySet()) {
			user = users.get(i);
			buffer = buffer + "\n\t" + user.toString();
		}
		return buffer;
	}

	private static String dump_categories(Map<Integer, Category> categories) {
		String buffer = new String();
		Category category;
		for (Integer i : categories.keySet()) {
			category = categories.get(i);
			buffer = buffer + "\n\t" + category.toString();
		}
		return buffer;
	}

	private static String dump_lines(Map<Integer, Line> lines) {
		String buffer = new String();
		Line line;
		for (Integer i : lines.keySet()) {
			line = lines.get(i);
			buffer = buffer + "\n\t" + line.toString();
		}
		return buffer;
	}

	private static String dump_tasks(Map<Integer, Task> tasks) {
		String buffer = new String();
		Task task;
		for (Integer i : tasks.keySet()) {
			task = tasks.get(i);
			buffer = buffer + "\n\t" + task.toString();
		}
		return buffer;
	}

}
