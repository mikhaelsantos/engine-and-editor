package com.unifina.data;

import java.util.Date;

public class FeedEvent implements Comparable<FeedEvent> {
	public Date timestamp;
	public Object content;
	public IEventRecipient recipient;
	public IFeed feed;
	public IHistoricalEventSource historicalSource;
	
	public long queueTicket = 0;
	
	@Override
	public int compareTo(FeedEvent e) {
		int t = timestamp.compareTo(e.timestamp);
		if (t!=0) return t;
		else return Long.compare(queueTicket, e.queueTicket);
	}
	
	@Override
	public String toString() {
		return timestamp + " - "+"parser: "+historicalSource+", content: "+content;
	}
}
