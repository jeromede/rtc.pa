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

package tests;

import rtc.model.Project;
import rtc.model.Task;
import rtc.model.Member;

public class Test {

	private static final String SER_FILE = "/tmp/pa1.ser";

	public static void main(String[] args) {
		Project p = new Project("UUID1", "pa1", "Project Area 1");
		p.putMember(new Member("UUID2", "j", "jérôme"));
		p.putMember(new Member("UUID3", "a", "Alice"));
		p.putMember(new Member("UUID4", "m", "Marion"));
		p.putTask(new Task("UUID5", 1, p.getMember("j"), null));
		p.putTask(new Task("UUID6", 2, p.getMember("m"), null));
		p.serialize(SER_FILE);
		Project q = Project.deserialize(SER_FILE);
		Task t = q.getTask("2");
		System.out.println(t.getId());
		Member u = t.getCreator();
		System.out.println(u.getName());
		p.dump(System.out);
	}

}
