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

import java.util.Date;
import java.util.List;

public abstract class Item implements java.io.Serializable {

	public static final String SEP = ", ";

	private static final long serialVersionUID = 8450163754687832796L;

	private static int nextInternal = Integer.MIN_VALUE;

	private Integer uid;
	private String sourceUUID = null;
	private transient String targetUUID = null;

	public String toString() {
		return trace("internal", uid) + SEP + trace("sourceUUID", sourceUUID);
	}

	public Item(String sourceUUID) {
		this.uid = nextInternal++;
		if (null != sourceUUID)
			this.sourceUUID = sourceUUID;
	}

	public int getUID() {
		return this.uid;
	}

	public String getSourceUUID() {
		return this.sourceUUID;
	}

	public String setTargetUUID(String id) {
		return this.targetUUID = id;
	}

	public String getTargetUUID() {
		return this.targetUUID;
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

	private static String trace(List<String> l) {
		String result = new String();
		for (String t : l) {
			if (!result.isEmpty()) {
				result += ", ";
			}
			result += '\"' + t + '\"';
		}
		return result;
	}

	public static String trace(String t, List<String> l) {
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
}
