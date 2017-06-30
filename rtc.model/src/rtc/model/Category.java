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

public class Category extends Item implements Serializable {

	private static final long serialVersionUID = -1423416328270173353L;

	private String name;
	private String hierarchicalName;
	private String description;

	public String toString() {
		return super.toString() + ", name: \"" + this.name + "\", hierarchicalName: \"" + this.hierarchicalName
				+ "\", description: \"" + this.description + "\"";
	}

	public Category(String name, String hierarchicalName, String description) {
		this(null, name, hierarchicalName, description);
	}

	public Category(String oldId, String name, String hierarchicalName, String description) {
		super(oldId);
		this.name = new String(name);
		this.hierarchicalName = new String(hierarchicalName);
		this.description = new String(description);
	}

	public String getName() {
		return this.name;
	}

	public String getHierarchicalName() {
		return this.hierarchicalName;
	}

	public String getDescription() {
		return this.description;
	}

}
