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

package rtc.pa.text;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TextSaver implements Serializable {

	private static final long serialVersionUID = 552302703535289825L;

	private Map<Integer, Text> values = new HashMap<Integer, Text>();
	private Map<String, Integer> indexes = new HashMap<String, Integer>();

	public TextSaver() {
		super();
	}

	public Text get(String s) {
		Integer i = indexes.get(s);
		if (null == i) {
			Text txt = new Text(s);
			i = txt.index();
			values.put(i, txt);
			indexes.put(txt.value(), i);
			return txt;
		}
		return values.get(i);
	}

}
