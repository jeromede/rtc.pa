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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Task extends Item implements Serializable {

	private static final long serialVersionUID = 1325028522439850990L;

	private String name;
	private Member member;
	private Map<Integer, TaskVersion> versions = new HashMap<Integer, TaskVersion>();
	private Map<String, TaskVersion> versions0 = new HashMap<String, TaskVersion>();

	public String toString() {
		return super.toString() + Item.SEP + Item.trace("name", name) + Item.SEP + Item.trace("member", member)
				+ Item.SEP + Item.trace_list("version", versionsToString());
	}

	private String versionsToString() {
		String result = "";
		TaskVersion v;
		int n = 0;
		for (Integer k : versions.keySet()) {
			v = versions.get(k);
			if (result.isEmpty()) {
				result = Item.trace(n++, v);
			} else {
				result = result + Item.SEP + Item.trace(n++, v);
			}
		}
		return result;
	}

	public Task(String sourceUUID, String name, Member member) {
		super(sourceUUID);
		this.name = new String(name);
		this.member = member;
	}

	public String getName() {
		return this.name;
	}

	public Member getUser() {
		return this.member;
	}

	public void putTaskVersion(TaskVersion version) {
		versions.put(version.getUID(), version);
		versions0.put(version.getSourceUUID(), version);
	}

	public TaskVersion getTaskVersion(int uid) {
		return versions.get(uid);
	}

	public TaskVersion getTaskVersion(String sourceUUID) {
		return versions0.get(sourceUUID);
	}

}
