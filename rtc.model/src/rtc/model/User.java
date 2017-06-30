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

public class User extends Item implements Serializable {

	private static final long serialVersionUID = -7116824774676758450L;

	private String name;
	private String userId;

	public String toString() {
		return super.toString() + ", name: \"" + this.name + "\", userId: \"" + this.userId + "\"";
	}

	public User(String name, String userid) {
		this(null, name, userid);
	}

	public User(String oldId, String name, String userId) {
		super(oldId);
		this.name = new String(name);
		this.userId = new String(userId);
	}

	public String getName() {
		return this.name;
	}

	public String getUserId() {
		return this.userId;
	}

}
