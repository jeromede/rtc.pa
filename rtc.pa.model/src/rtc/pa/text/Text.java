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

package rtc.pa.text;

import java.io.Serializable;

public class Text implements Serializable {

	private static final long serialVersionUID = -7594305309006190632L;

	private static int nextInternal = Integer.MIN_VALUE;
	
	private int index;
	private String value;
	
	public String toString() {
		return this.value();
	}
	
	public String value() {
		return this.value;
	}

	Text(String value) {
		super();
		this.index = nextInternal++;
		this.value = new String(value);
	}

	int index() {
		return this.index;
	}

	public static boolean same(Text t1, Text t2) {
		return t1.index == t2.index;
	}

}
