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

public class Approval extends Item implements Serializable {

	private static final long serialVersionUID = -7116824774676758450L;

	private Member approver;
	private String name;
	// TODO: rest of properties

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("approver", approver)//
				+ Item.SEP + Item.trace("name", name);
	}

	public Approval(//
			String sourceId, //
			Member approver, //
			String name) {
		super(sourceId);
		this.approver = approver;
		this.name = name;
	}

	public Member getApprover() {
		return this.approver;
	}

	public String getName() {
		return this.name;
	}

}