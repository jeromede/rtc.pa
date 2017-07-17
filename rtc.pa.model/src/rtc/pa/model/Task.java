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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

public class Task extends Item implements Serializable {

	private static final long serialVersionUID = 1325028522439850990L;

	private int id;
	private Member creator;
	private Date creation;
	private SortedMap<Date, TaskVersion> history = new TreeMap<Date, TaskVersion>();

	private Collection<Link> links = new ArrayList<Link>();

	private Collection<Artifact> artifacts = new ArrayList<Artifact>();

	private Collection<Attachment> attachments = new ArrayList<Attachment>();

	private Collection<Approval> approvals = new ArrayList<Approval>();

	public String toString() {
		return super.toString()//
				+ Item.trace("id", id)//
				+ Item.trace("creator", creator)//
				+ Item.trace("creation date", creation)//
				+ Item.trace_list("\nLINKS", itemsToString(links))//
				+ Item.trace_list("\nATTACHMENTS", Item.itemsToString(attachments))//
				+ Item.trace_list("\nARTIFACTS", Item.itemsToString(artifacts))//
				+ Item.trace_list("\nAPPROVALS", Item.itemsToString(approvals))//
				+ Item.trace_list("\nVERSIONS", Item.itemsToString(history.values()));
	}

	public Task(//
			String sourceId, //
			int id, //
			Member creator, //
			Date creation) {
		super(sourceId);
		this.id = id;
		this.creator = creator;
		this.creation = creation;
	}

	public int getId() {
		return this.id;
	}

	public Member getCreator() {
		return this.creator;
	}

	public Date getCreation() {
		return this.creation;
	}

	void putTaskVersion(TaskVersion version) {
		history.put(version.getModified(), version);
	}

	public Collection<TaskVersion> getHistory() {
		return history.values();
	}

	public void addLink(Link link) {
		links.add(link);
	}

	public Collection<Link> getLinks() {
		return links;
	}

	public void addArtifact(Artifact artifact) {
		artifacts.add(artifact);
	}

	public Collection<Artifact> getArtifacts() {
		return artifacts;
	}

	public void addApproval(Approval approval) {
		approvals.add(approval);
	}

	public Collection<Approval> getApproval() {
		return approvals;
	}

	public void addAttachment(Attachment attachment) {
		attachments.add(attachment);
	}

	public Collection<Attachment> getAttachment() {
		return attachments;
	}

}
