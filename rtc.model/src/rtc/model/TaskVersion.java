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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class TaskVersion extends Item implements Serializable {

	private static final long serialVersionUID = -7763110461126796356L;

	private String type;
	private Member modifier;
	private Date modified;
	private String description;
	private String summary;
	private String priority;
	private String severity;
	private List<String> tags;
	private Timestamp due;
	private long duration;
	private Category category;
	private Iteration target;
	private Member ownedBy;
	private Member resolvedBy;
	private Date resolution;

	public String toString() {
		return super.toString() + Item.SEP + Item.trace("name", type) + Item.SEP + Item.trace("modifier", modifier)
				+ Item.SEP + Item.trace("modified", modified) + Item.SEP + Item.trace("description", description)
				+ Item.SEP + Item.trace("summary", summary) + Item.SEP + Item.trace("priority", priority) + Item.SEP
				+ Item.trace("severity", severity) + Item.SEP + Item.trace("tags", tags) + Item.SEP
				+ Item.trace("due", due) + Item.SEP + Item.trace("duration", duration) + Item.SEP
				+ Item.trace("category", category) + Item.SEP + Item.trace("target", target) + Item.SEP
				+ Item.trace("ownedBy", ownedBy) + Item.SEP + Item.trace("resolvedBy", resolvedBy) + Item.SEP
				+ Item.trace("resolution", resolution);
	}

	public TaskVersion(String sourceUUID, String type, Member modifier, Date modified, String description,
			String summary, String priority, String severity, List<String> tags, Timestamp due, long duration,
			Category category, Iteration target, Member ownedBy, Member resolvedBy, Date resolution) {
		super(sourceUUID);
		this.type = type;
		this.modifier = modifier;
		this.modified = modified;
		this.description = description;
		this.summary = summary;
		this.priority = priority;
		this.severity = severity;
		this.tags = tags;
		this.due = due;
		this.duration = duration;
		this.category = category;
		this.target = target;
		this.ownedBy = ownedBy;
		this.resolvedBy = resolvedBy;
		this.resolution = resolution;
	}

	public String getType() {
		return this.type;
	}

	public Member getModifier() {
		return this.modifier;
	}

	public Date getModified() {
		return this.modified;
	}

	public String getDescription() {
		return this.description;
	}

	public String getSummary() {
		return this.summary;
	}

	public String getPriority() {
		return this.priority;
	}

	public String getSeverity() {
		return this.severity;
	}

	public List<String> getTags() {
		return this.tags;
	}

	public Timestamp getDue() {
		return this.due;
	}

	public long getDuration() {
		return this.duration;
	}

	public Category getCategory() {
		return this.category;
	}

	public Iteration getTarget() {
		return this.target;
	}

	public Member getOwnedBy() {
		return this.ownedBy;
	}

	public Member getResolvedBy() {
		return this.resolvedBy;
	}

	public Date getResolution() {
		return this.resolution;
	}

}
