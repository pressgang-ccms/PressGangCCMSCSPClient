package com.redhat.contentspec.client.entities;

import java.util.ArrayList;
import java.util.List;

public class SnapshotList {

	private List<Snap> snapshots;
	private long count = 0;
	
	public SnapshotList() {
		snapshots = new ArrayList<Snap>();
	}
	
	public SnapshotList(List<Snap> snapshots) {
		this.snapshots = snapshots;
		count = snapshots.size();
	}
	
	public SnapshotList(long count) {
		snapshots = new ArrayList<Snap>();
		this.count = count;
	}
	
	public SnapshotList(List<Snap> snapshots, long count) {
		this.snapshots = snapshots;
		this.count = count;
	}
	
	public List<Snap> getSnaps() {
		return snapshots;
	}
	
	public void addSpec(Snap snapshot) {
		snapshots.add(snapshot);
		count++;
	}

	public long getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public String toString() {
		final StringBuilder output = new StringBuilder("Number of Snapshots: " + count + "\n");
		for (final Snap snapshot: snapshots) {
			output.append(snapshot.toString() + "\n");
		}
		return output.toString();
	}
}
