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
import java.util.HashMap;
import java.util.Map;

public class Attribute extends Item implements Serializable {

	private static final long serialVersionUID = 3735492808454873371L;

	private String name;
	private String type;
	private Map<String, Literal> literals = null;
	private Literal nullLiteral;

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("name", this.getName())//
				+ Item.SEP + Item.trace("type", this.getType())//
				+ Item.SEP + Item.trace("enum?", this.isEnum());
	}

	public Attribute(//
			String sourceId, //
			String name, //
			String type) {
		super(sourceId);
		this.name = name;
		this.type = type;
	}

	public Attribute(//
			String sourceUUID, //
			String name, //
			String type, //
			Collection<Literal> literals, //
			Literal nullLiteral) {
		super(sourceUUID);
		this.name = name;
		this.type = type;
		this.literals = new HashMap<String, Literal>();
		for (Literal l : literals) {
			this.literals.put(l.getName(), l);
		}
		this.nullLiteral = nullLiteral;
	}

	public String getName() {
		return this.name;
	}

	public String getType() {
		return this.type;
	}

	public boolean isEnum() {
		return null != this.literals;
	}

	public Collection<Literal> getLiterals() {
		return this.literals.values();
	}

	public Literal getLiteral(String name) {
		return this.literals.get(name);
	}

	public Literal getNullLiteral() {
		return this.nullLiteral;
	}

}
