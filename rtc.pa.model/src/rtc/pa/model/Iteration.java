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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Iteration extends Item implements Serializable {

	private static final long serialVersionUID = 7283835080445170873L;

	private static int nextInternal = 0;

	private String name;
	private String id;
	private String alternateId;
	private String label;
	private String description;
	private Date starts;
	private Date ends;
	private boolean hasDeliverable;
	private Map<String, Iteration> iterations = new HashMap<String, Iteration>();

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("original id", id)//
				+ Item.SEP + Item.trace("alternate id", alternateId)//
				+ Item.SEP + Item.trace("name", name)//
				+ Item.SEP + Item.trace("label", label)//
				+ Item.SEP + Item.trace("description", description)//
				+ Item.SEP + Item.trace("starts", starts)//
				+ Item.SEP + Item.trace("ends", ends)//
				+ Item.SEP + Item.trace("has deliverable", hasDeliverable) //
				+ Item.SEP + Item.trace_list("\nSUBITERATIONS", Item.itemsToString(iterations.values()));
	}

	public Iteration(//
			String sourceId, //
			String name, //
			String id, //
			String label, //
			String description, //
			Date starts, //
			Date ends, //
			boolean hasDeliverable) {
		super(sourceId);
		this.id = id;
		this.alternateId = id + ' ' + nextInternal;
		this.name = name + ' ' + nextInternal++;
		this.label = label;
		this.description = description;
		this.starts = starts;
		this.ends = ends;
	}

	public String getName() {
		return this.name;
	}

	public String getId() {
		return this.id;
	}

	public String getAlternateId() {
		return this.alternateId;
	}

	public String getLabel() {
		return this.label;
	}

	public String getDescription() {
		return this.description;
	}

	public Date getStarts() {
		return this.starts;
	}

	public Date getEnds() {
		return this.ends;
	}

	public boolean hasDeliverable() {
		return this.hasDeliverable;
	}

	void putIteration(Iteration iteration) {
		iterations.put(iteration.getId(), iteration);
	}

	public Collection<Iteration> getIterations() {
		return iterations.values();
	}

}
