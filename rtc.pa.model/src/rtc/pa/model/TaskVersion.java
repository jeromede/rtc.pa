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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import rtc.pa.text.Text;

public class TaskVersion extends Item implements Serializable {

	private static final long serialVersionUID = -7763110461126796356L;

	private Task task;
	private TaskType type;
	private String state;
	private Member modifier;
	private Date modified;
	private Text summary;
	private Text description;
	private Text priority;
	private Text severity;
	private Collection<String> tags;
	private Timestamp due;
	private long duration; // -1 means null
	private Category category;
	private Iteration target;
	private Member ownedBy;
	private Member resolvedBy;
	private Date resolution;
	private String resolution2;
	private Collection<Value> values = new ArrayList<Value>();
	private Collection<Comment> comments = new ArrayList<Comment>();
	private Collection<Member> subscribers = new ArrayList<Member>();

	public String toString() {
		return super.toString()//
				+ Item.SEP + Item.trace("type", type)//
				+ Item.SEP + Item.trace("state", state)//
				+ Item.SEP + Item.trace_simple("modifier", (null == modifier) ? null : modifier.getUserId())//
				+ Item.SEP + Item.trace("modified", modified)//
				+ Item.SEP + Item.trace("summary", summary.value())//
				+ Item.SEP + Item.trace("description", description.value())//
				+ Item.SEP + Item.trace_simple("priority", priority.value())//
				+ Item.SEP + Item.trace_simple("severity", severity.value())//
				+ Item.SEP + Item.trace("tags", tags)//
				+ Item.SEP + Item.trace("due", due)//
				+ Item.SEP + Item.trace("duration", duration)//
				+ Item.SEP + Item.trace("category", category)//
				+ Item.SEP + Item.trace_simple("target", (null == target) ? null : target.getAlternateId())//
				+ Item.SEP + Item.trace_simple("owned by", (null == ownedBy) ? null : ownedBy.getUserId())//
				+ Item.SEP + Item.trace_simple("resolved by", (null == resolvedBy) ? null : resolvedBy.getUserId())//
				+ Item.SEP + Item.trace("resolution", resolution)//
				+ Item.SEP + Item.trace("resolution2", resolution2)//
				+ Item.trace_list("\nVALUES", Item.itemsToString(values))//
				+ Item.trace_list("\nSUBSCRIBERS", Item.itemsToString(subscribers))//
				+ Item.trace_list("\nCOMMENTS", Item.itemsToString(comments));
	}

	public TaskVersion(//
			String sourceId, //
			Task task, //
			TaskType type, //
			String state, //
			Member modifier, //
			Date modified, //
			Text summary, //
			Text description, //
			Text priority, //
			Text severity, //
			List<String> tags, //
			Timestamp due, //
			long duration, //
			Category category, //
			Iteration target, //
			Member ownedBy, //
			Member resolvedBy, //
			Date resolution, //
			String resolution2) {
		super(sourceId);
		this.task = task;
		this.type = type;
		this.state = state;
		this.modifier = modifier;
		this.modified = modified;
		this.summary = summary;
		this.description = description;
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
		this.resolution2 = resolution2;
	}

	public TaskVersion clone() {
		return new TaskVersion(this.getSourceId() + "1", this.task, this.type, this.state, this.modifier, this.modified,
				this.summary, this.description, this.priority, this.severity, (List<String>) this.tags, this.due,
				this.duration, this.category, this.target, this.ownedBy, this.resolvedBy, this.resolution,
				this.resolution2);
	}

	public TaskVersion change(Project p, TaskType newType) {
		this.type = newType;
		this.summary = p.saver().get(this.summary + " (process changed)");
		return this;
	}

	public Task getTask() {
		return this.task;
	}

	public TaskType getType() {
		return this.type;
	}

	public String getState() {
		return this.state;
	}

	public Member getModifier() {
		return this.modifier;
	}

	public Date getModified() {
		return this.modified;
	}

	public String getDescription() {
		return this.description.value();
	}

	public String getSummary() {
		return this.summary.value();
	}

	public String getPriority() {
		return this.priority.value();
	}

	public String getSeverity() {
		return this.severity.value();
	}

	public Collection<String> getTags() {
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

	public String getResolution2() {
		return this.resolution2;
	}

	public void addValue(Value value) {
		values.add(value);
	}

	public Collection<Value> getValues() {
		return values;
	}

	public void addSubscriber(Member subscriber) {
		subscribers.add(subscriber);
	}

	public Collection<Member> getSubscribers() {
		return subscribers;
	}

	public void addComment(Comment comment) {
		comments.add(comment);
	}

	public Collection<Comment> getComments() {
		return comments;
	}

	public boolean isOfType(String typeId) {
		return this.type.getSourceId().equals(typeId);
	}

}
