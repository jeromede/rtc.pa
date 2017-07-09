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
import java.util.HashMap;
import java.util.Map;

public class Text implements Serializable {

	private static final long serialVersionUID = -7594305309006190632L;

	private int index;
	private static int nextInternal = Integer.MIN_VALUE;

	private static Map<Integer, String> values = new HashMap<Integer, String>();
	private static Map<String, Integer> indexes = new HashMap<String, Integer>();

	public String toString() {
		return this.value();
	}

	private Text() {
		this.index = nextInternal++;
	}

	private Text(int index) {
		this.index = index;
	}

	public static Text get(String s) {
//		System.out.println("\nget(\"" + s + "\")");
		Integer i = indexes.get(s);
		if (null == i) {
			String value = new String(s);
			Text text = new Text();
			values.put(text.index, value);
			indexes.put(value, text.index);
//			System.out.println("values: " + values);
//			System.out.println("indexes: " + indexes);
			return text;
		} else {
//			System.out.println("values: " + values);
//			System.out.println("indexes: " + indexes);
			return new Text(i);
		}
	}

	public String value() {
//		System.out.println("\nvalue()");
//		System.out.println("values: " + values);
//		System.out.println("indexes: " + indexes);
		return values.get(this.index);
	}
	
	public static boolean same(Text t1, Text t2) {
		return t1.index == t2.index;
	}

}
