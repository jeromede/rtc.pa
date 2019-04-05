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

import java.util.Collection;
import java.util.Date;

public abstract class Item implements java.io.Serializable {

	public static final String SEP = ", ";

	private static final long serialVersionUID = 8450163754687832796L;

	private String sourceId;
	private transient String externalId = null;
	private transient Object externalObject = null;

	public String toString() {
		return trace("source id", sourceId);
	}

	public static String itemsToString(Collection<? extends Item> items) {
		String result = new String();
		int n = 0;
		for (Item i : items) {
			if (result.isEmpty()) {
				result = Item.trace(n++, i);
			} else {
				result = result + Item.SEP + Item.trace(n++, i);
			}
		}
		return result;
	}

	public Item(String id) {
		this.sourceId = id;
	}

	public Item change(String newId) {
		this.sourceId = newId;
		return this;
	}

	public String getSourceId() {
		return this.sourceId;
	}

	public void setExternalObject(String id, Object o) {
		this.externalId = id;
		this.externalObject = o;
	}

	public String getExternalId() {
		return this.externalId;
	}

	public Object getExternalObject() {
		return this.externalObject;
	}

	public static String trace(String t, boolean b) {
		return '\n' + t + ": ?" + b + '?';
	}

	public static String trace(String t, int i) {
		return '\n' + t + ": \"" + i + '\"';
	}

	public static String trace(String t, long i) {
		return '\n' + t + ": \"" + i + '\"';
	}

	public static String trace(String t, String s) {
		if (null == s) {
			return '\n' + t + ": null";
		}
		return '\n' + t + ": \"" + s + '\"';
	}

	private static String trace(Collection<String> c) {
		String result = new String();
		for (String s : c) {
			if (!result.isEmpty()) {
				result += ", ";
			}
			result += '\"' + s + '\"';
		}
		return result;
	}

	public static String trace(String t, Collection<String> l) {
		if (null == l) {
			return '\n' + t + ": null";
		}
		return '\n' + t + ": [" + trace(l) + ']';
	}

	public static String trace_simple(String t, String s) {
		if (null == s) {
			return '\n' + t + ": null";
		}
		return '\n' + t + ": " + s;
	}

	public static String trace(String t, Date d) {
		if (null == d) {
			return '\n' + t + "-> null";
		}
		return '\n' + t + ": /" + d + '/';
	}

	public static String trace(String t, Item k) {
		if (null == k) {
			return '\n' + t + ": null";
		}
		return '\n' + t + ": {" + k + '}';
	}

	public static String trace(int i, Item k) {
		if (null == k) {
			return "\n\n" + i + "-> null";
		}
		return "\n\n" + i + "-> {" + k + '}';
	}

	public static String trace_list(String t, Object o) {
		if (null == o) {
			return '\n' + t + ": null";
		}
		return '\n' + t + ": [" + o + ']' + '\n';
	}

	public static String trace_object(String t, Object o) {
		if (null == o) {
			return '\n' + t + ": null";
		}
		return '\n' + t + ": {{{" + o + "}}}";
	}

}
