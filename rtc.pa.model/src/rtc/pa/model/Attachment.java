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
import java.sql.Timestamp;

public class Attachment extends Item implements Serializable {

	private static final long serialVersionUID = 8551811459594967567L;

	private String name;
	private String description;
	private Member creator;
	private Timestamp creation;

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("name", name)//
				+ Item.SEP + Item.trace("description", description)//
				+ Item.SEP + Item.trace("creator", creator)//
				+ Item.SEP + Item.trace("creation date", creation);
	}

	public Attachment(//
			String id, //
			String name, //
			String description, //
			Member creator, //
			Timestamp creation) {
		super(id);
		this.name = name;
		this.description = description;
		this.creator = creator;
		this.creation = creation;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public Member getCreator() {
		return this.creator;
	}

	public Timestamp getCreation() {
		return this.creation;
	}

}
