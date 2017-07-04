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

public class Line extends Item implements Serializable {

	private static final long serialVersionUID = 6185617986682800843L;

	private String name;
	private Map<Integer, Iteration> iterations = new HashMap<Integer, Iteration>();
	private Map<String, Iteration> iterations0 = new HashMap<String, Iteration>();

	public String toString() {
		return super.toString() + Item.SEP + Item.trace("name", name) + Item.SEP
				+ Item.trace("iterations", iterationsToString());
	}

	private String iterationsToString() {
		String result = "";
		Iteration i;
		int n = 0;
		for (Integer k : iterations.keySet()) {
			i = iterations.get(k);
			if (result.isEmpty()) {
				result = Item.trace(n++, i);
			} else {
				result = result + Item.SEP + Item.trace(n++, i);
			}
		}
		return result;
	}

	public Line(String name) {
		this(null, name);
	}

	public Line(String oldId, String name) {
		super(oldId);
		this.name = new String(name);
	}

	public String getName() {
		return this.name;
	}

	public void putIteration(Iteration iteration) {
		iterations.put(iteration.getId(), iteration);
		iterations0.put(iteration.getOldId(), iteration);
	}

	public Iteration getIteration(int id) {
		return iterations.get(id);
	}

	public Iteration getIteration(String id) {
		return iterations0.get(id);
	}

}
