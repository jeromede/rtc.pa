/*
 * Copyright (c) 2017,2018,2019 jeromede@fr.ibm.com
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

package rtc.pa.model.utilities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtc.pa.model.Link;
import rtc.pa.model.Project;
import rtc.pa.model.Task;
import rtc.pa.model.TaskType;
import rtc.pa.model.TaskVersion;

public abstract class ChangeProcess {

	@SuppressWarnings("unused")
	private static void trace() {
		System.out.println();
	}

	@SuppressWarnings("unused")
	private static void trace(String s) {
		System.out.println(s);
	}

	@SuppressWarnings("unused")
	private static void trace(String t, String s) {
		System.out.println(t + ": " + s);
	}

	@SuppressWarnings("unused")
	private static void trace(String t, int i) {
		System.out.println(t + ": " + i);
	}

	@SuppressWarnings("unused")
	private static void trace(String t, int i, int j) {
		System.out.println(t + ": " + i + " | " + j);
	}

	@SuppressWarnings("unused")
	private static void trace(String t, String s1, String s2) {
		System.out.println(t + ": \"" + s1 + "\" | \"" + s2 + '\"');
	}

	public static void main(String[] args) throws IOException {

		String ser1, ser2;
		// String mapping;
		try {
			ser1 = new String(args[0]);
			ser2 = new String(args[1]);
			// mapping = new String(args[2]);
		} catch (Exception e) {
			System.err.println("arguments: source.ser target.ser mapping");
			System.err.print("Wrong arguments:");
			for (String arg : args) {
				System.err.println(arg);
			}
			System.err.println();
			return;
		}
		Project p = Project.deserialize(ser1);
		trace("About to modify project image for " + p.getName());
		String result = update(p);
		if (null != result) {
			trace("ERROR: " + result);
			return;
		}
		p.serialize(ser2);
	}

	private static String update(Project p) {
		String result = null;
		TaskType tt = p.getTaskType("com.ibm.team.apt.workItemType.story");
		tt.change("com.ibm.team.apt.workItemType.story.bis", "Enhancement");
		Task t = p.getTask(277);
		TaskVersion v = null;
		for (TaskVersion w : t.getHistory()) {
			v = w;
		}
		v = v.clone();
		v.change(p, tt);
		p.putTaskVersion(v);
		return result;
	}

}
