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

public abstract class Item implements java.io.Serializable {

	private static final long serialVersionUID = 8450163754687832796L;

	private static int nextId = Integer.MIN_VALUE;

	private Integer id;
	private String oldId = null;
	private transient String newId = null;

	public String toString() {
		return "id: \"" + this.id + "\", oldId: \"" + this.oldId + "\"";
	}

	public Item(String oldId) {
		this.id = nextId++;
		if (null != oldId)
			this.oldId = new String(oldId);
	}

	public int getId() {
		return this.id;
	}

	public String setOldId(String id) {
		return this.oldId = new String(id);
	}

	public String getOldId() {
		return this.oldId;
	}

	public String setNewId(String id) {
		return this.newId = new String(id);
	}

	public String getNewId() {
		return this.newId;
	}

}