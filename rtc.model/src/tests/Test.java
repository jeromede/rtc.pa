package tests;

import rtc.model.Project;
import rtc.model.Task;
import rtc.model.User;

public class Test {

	public static void main(String[] args) {
		Project p = new Project("pa1", "Project Area 1");
		p.putUser(new User("j", "jérôme"));
		p.putUser(new User("a", "Alice"));
		p.putUser(new User("m", "Marion"));
		p.putTask(new Task("1", "do this", p.getUser("j")));
		p.putTask(new Task("2", "do that", p.getUser("m")));
		//p.serialize("/tmp/pa1.ser");
		Project q = Project.deserialize("/tmp/pa1.ser");
		Task t = q.getTask("2");
		System.out.println(t.getName());
		User u = t.getUser();
		System.out.println(u.getName());
		System.out.println(p.dump());
	}

}
