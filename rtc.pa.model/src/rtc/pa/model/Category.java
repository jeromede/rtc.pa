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

public class Category extends Item implements Serializable {

	private static final long serialVersionUID = -1423416328270173353L;

	private String name;
	private String hierarchicalName;
	private String description;
	private String parentId;

	public String toString() {
		return super.toString() //
				+ Item.SEP + Item.trace("name", name) //
				+ Item.SEP + Item.trace("hierarchicalName", hierarchicalName) //
				+ Item.SEP + Item.trace("description", description)//
				+ Item.SEP + Item.trace("parentId", parentId);
	}

	public Category(//
			String sourceCategoryId, //
			String name, //
			String hierarchicalName, //
			String description, //
			String parentId) {
		super(sourceCategoryId);
		this.name = name;
		this.hierarchicalName = hierarchicalName;
		this.description = description;
		if (parentId.equals("/Unassigned/")) {
			this.parentId = null;
		} else {
			this.parentId = parentId;
		}
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

	public String getParentId() {
		return this.parentId;
	}

}
