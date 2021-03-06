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

package rtc.pa.model;

import java.io.Serializable;
import java.sql.Timestamp;

import rtc.pa.text.Text;

public class Comment extends Item implements Serializable {

	private static final long serialVersionUID = 8342719141680496561L;

	private int id;
	private Member creator;
	private Timestamp creation;
	private Text content;

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("id", id)//
				+ Item.SEP + Item.trace("creator", creator)//
				+ Item.SEP + Item.trace("creation date", creation)//
				+ Item.SEP + Item.trace("content", content.value());
	}

	public Comment(//
			String id, //
			Member creator, //
			Timestamp creation, //
			Text content) {
		super(id);
		this.creator = creator;
		this.creation = creation;
		this.content = content;
	}

	public Member getCreator() {
		return this.creator;
	}

	public Timestamp getCreation() {
		return this.creation;
	}

	public String getContent() {
		return this.content.value();
	}

}
