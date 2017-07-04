package rtc.model.trace;

import rtc.model.Project;

public abstract class Main {

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
		System.out.println(p.dump());
	}

}
