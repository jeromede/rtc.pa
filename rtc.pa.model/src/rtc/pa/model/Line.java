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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class /* Development */ Line extends Item implements Serializable {

	//
	// Timeline constraints:
	// - unique identifier for development lines
	// - unique identifier for iterations in a given development line
	// - can't change the identifier of a development line or an iteration
	// - can't delete a development line or an iteration.
	// So: ideally reuse the existing development lines / iterations
	// and create new ones only if necessary.
	// But they could have been re-organised...
	// Instead: archive everything that exists
	// and add the source project area name as a prefix.
	//

	private static final long serialVersionUID = 6185617986682800843L;

	private static int nextInternal = 0;

	private String name;
	private String id;
	private String alternateId;
	private Date starts;
	private Date ends;
	private boolean projectLine;
	private Map<String, Iteration> iterations = new HashMap<String, Iteration>();
	private Iteration current = null;

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("original id", id)//
				+ Item.SEP + Item.trace("alternate id", alternateId)//
				+ Item.SEP + Item.trace("name", name)//
				+ Item.SEP + Item.trace("starts", starts)//
				+ Item.SEP + Item.trace("ends", ends)//
				+ Item.SEP + Item.trace("project line", projectLine)//
				+ Item.SEP + Item.trace("current", (null == current) ? null : current.getName())//
				+ Item.SEP + Item.trace_list("\nITERATIONS", iterationsToString());
	}

	private String iterationsToString() {
		String result = new String();
		int n = 0;
		for (Iteration i : iterations.values()) {
			if (result.isEmpty()) {
				result = Item.trace(n++, i);
			} else {
				result = result + Item.SEP + Item.trace(n++, i);
			}
		}
		return result;
	}

	public Line(//
			String sourceId, //
			String id, //
			String name, //
			Date starts, //
			Date ends, //
			boolean projectLine) {
		super(sourceId);
		this.id = id;
		this.alternateId = id + " {" + nextInternal + '}';
		this.name = name + " {" + nextInternal++ + '}';
		this.starts = starts;
		this.ends = ends;
		this.projectLine = projectLine;
	}

	public String getId() {
		return this.id;
	}

	public String getAlternateId() {
		return this.alternateId;
	}

	public String getName() {
		return this.name;
	}

	public Date getStarts() {
		return this.starts;
	}

	public Date getEnds() {
		return this.ends;
	}

	public boolean isProjectLine() {
		return this.projectLine;
	}

	void putIteration(Iteration iteration) {
		iterations.put(iteration.getSourceId(), iteration);
	}

	public Iteration getIteration(String sourceId) {
		return iterations.get(sourceId);
	}

	public Collection<Iteration> getIterations() {
		return iterations.values();
	}

	public void setCurrent(Iteration current) {
		this.current = current;
	}

	public Iteration getCurrent() {
		return this.current;
	}

}
