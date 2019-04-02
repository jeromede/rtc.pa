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

public class Approval extends Item implements Serializable {

	private static final long serialVersionUID = -7116824774676758450L;

	private String type;
	private String state;
	private Timestamp due;
	private Member approver;

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("type", type)//
				+ Item.SEP + Item.trace("state", state)//
				+ Item.SEP + Item.trace("due date", due)//
				+ Item.SEP + Item.trace("approver", approver);
	}

	public Approval(//
			String sourceId, // ,
			String type, //
			String state, //
			Timestamp due, //
			Member approver) {
		super(sourceId);
		this.type = type;
		this.state = state;
		this.due = due;
		this.approver = approver;
	}

	public String getType() {
		return this.type;
	}

	public Timestamp getDue() {
		return this.due;
	}

	public Member getApprover() {
		return this.approver;
	}
	
	public String getState() {
		return this.state;
	}

}
