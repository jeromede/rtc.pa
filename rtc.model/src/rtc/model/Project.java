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
	private Map<Integer, User> users = new HashMap<Integer, User>();
	private Map<Integer, Task> tasks = new HashMap<Integer, Task>();
	private Map<String, User> users0 = new HashMap<String, User>();
	private Map<String, Task> tasks0 = new HashMap<String, Task>();

	public Project(String name) {
		super();
		this.name = new String(name);
	}

	public Project(String oldId, String name) {
		super(oldId);
		this.name = new String(name);
	}

	public String getName() {
		return this.name;
	}

	public void putUser(User user) {
		users.put(user.getId(), user);
		users0.put(user.getOldId(), user);
	}

	public User getUser(int id) {
		return users.get(id);
	}

	public User getUser(String id) {
		return users0.get(id);
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

	public String dump() {
		return "project id: " + this.getId() + "\nproject name: " + this.name + "\nproject users: "
				+ dump_users(this.users) + "\nproject tasks: " + dump_tasks(this.tasks);
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

	private static String dump_users(Map<Integer, User> users) {
		String buffer = new String();
		User user;
		for (Integer i : users.keySet()) {
			user = users.get(i);
			buffer = buffer + "\nuser id: " + user.getId() + "\nuser old id: " + user.getOldId() + "\nuser name: "
					+ user.getName();
		}
		return buffer;
	}

	private static String dump_tasks(Map<Integer, Task> tasks) {
		String buffer = new String();
		Task task;
		for (Integer i : tasks.keySet()) {
			task = tasks.get(i);
			buffer = buffer + "\ntask id: " + task.getId() + "\ntask old id: " + task.getOldId() + "\ntask name: "
					+ task.getName();
		}
		return buffer;
	}

}
