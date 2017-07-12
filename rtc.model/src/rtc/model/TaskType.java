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

public class TaskType extends Item implements Serializable {

	private static final long serialVersionUID = 2213957782973299588L;

	private String name;

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("name", name);
	}

	public TaskType(//
			String sourceId, //
			String name) {
		super(sourceId);
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
