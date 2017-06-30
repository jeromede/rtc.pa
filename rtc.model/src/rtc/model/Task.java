package rtc.model;

import java.io.Serializable;

public class Task extends Item implements Serializable {

	private static final long serialVersionUID = 1325028522439850990L;

	private String name;
	private User user;

	public Task(String name, User user) {
		super();
		this.name = new String(name);
		this.user = user;
	}
	
	public Task(String oldId, String name, User user) {
		super(oldId);
		this.name = new String(name);
		this.user = user;
	}

	public String getName() {
		return this.name;
	}

	public User getUser() {
		return this.user;
	}

}
