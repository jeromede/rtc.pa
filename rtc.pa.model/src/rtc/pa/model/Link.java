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

public class Link extends Item implements Serializable {

	private static final long serialVersionUID = 4250604954024032992L;

	private transient String targetId;
	private Task target = null;
	private String type;
	private String comment;

	public String toString() {
		if (null != this.targetId) {
			return super.toString()//
					+ Item.SEP + Item.trace("target id", targetId)//
					+ Item.SEP + Item.trace("type", type)//
					+ Item.SEP + Item.trace("comment", comment);
		} else {
			return super.toString()//
					+ Item.SEP + Item.trace("target", target.getId())//
					+ Item.SEP + Item.trace("type", type)//
					+ Item.SEP + Item.trace("comment", comment);
		}
	}

	public Link(//
			String sourceId, //
			String targetId, //
			String type, //
			String comment) {
		super(sourceId);
		this.targetId = targetId;
		this.type = type;
	}

	public String getTargetId() {
		return this.targetId;
	}

	public String getType() {
		return this.type;
	}

	public String getComment() {
		return this.comment;
	}

	public void resolve(Task target) {
		this.target = target;
		this.targetId = null;
	}

	public Task getTarget() {
		return target;
	}

}
