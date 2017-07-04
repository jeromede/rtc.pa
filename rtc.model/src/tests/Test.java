package tests;

import rtc.model.Project;
import rtc.model.Task;
import rtc.model.Member;

public class Test {

	private static final String SER_FILE = "/tmp/pa1.ser";

	public static void main(String[] args) {
		Project p = new Project("pa1", "Project Area 1");
		p.putMember(new Member("j", "jérôme"));
		p.putMember(new Member("a", "Alice"));
		p.putMember(new Member("m", "Marion"));
		p.putTask(new Task("1", "do this", p.getMember("j")));
		p.putTask(new Task("2", "do that", p.getMember("m")));
		p.serialize(SER_FILE);
		Project q = Project.deserialize(SER_FILE);
		Task t = q.getTask("2");
		System.out.println(t.getName());
		Member u = t.getUser();
		System.out.println(u.getName());
		System.out.println(p.dump());
	}

}
