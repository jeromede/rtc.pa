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

package rtc.pa.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

public class Task extends Item implements Serializable {

	private static final long serialVersionUID = 1325028522439850990L;

	private int id;
	private Member creator;
	private Date creation;
	private SortedMap<Date, TaskVersion> history = new TreeMap<Date, TaskVersion>();

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("id", id)//
				+ Item.SEP + Item.trace("creator", creator)//
				+ Item.SEP + Item.trace("creation date", creation)//
				+ Item.SEP + Item.trace_list("\nVERSIONS", historyToString());
	}

	private String historyToString() {
		String result = new String();
		int n = 0;
		for (TaskVersion v : history.values()) {
			if (result.isEmpty()) {
				result = Item.trace(n++, v);
			} else {
				result = result + Item.SEP + Item.trace(n++, v);
			}
		}
		return result;
	}

	public Task(//
			String sourceId, //
			int id, //
			Member creator, //
			Date creation) {
		super(sourceId);
		this.id = id;
		this.creator = creator;
		this.creation = creation;
	}

	public int getId() {
		return this.id;
	}

	public Member getCreator() {
		return this.creator;
	}

	public Date getCreation() {
		return this.creation;
	}

	void putTaskVersion(TaskVersion version) {
		history.put(version.getModified(), version);
	}

	public Collection<TaskVersion> getHistory() {
		return history.values();
	}

}