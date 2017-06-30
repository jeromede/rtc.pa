package rtc.model;

import java.io.Serializable;

public class User extends Item implements Serializable {
	
	private static final long serialVersionUID = -8469149376708588419L;

	private String name;

	public User(String name) {
		super();
		this.name = new String(name);
	}
	public User(String oldId, String name) {
		super(oldId);
		this.name = new String(name);
	}
	
	public String getName() {
		return this.name;
	}

}
