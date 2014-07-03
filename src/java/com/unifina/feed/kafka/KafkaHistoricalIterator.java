package com.unifina.feed.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;

import com.unifina.feed.util.RawMessageIterator;
import com.unifina.kafkaclient.UnifinaKafkaMessageFactory;

public class KafkaHistoricalIterator implements Iterator<Object> {

	private RawMessageIterator rawIterator;
	private KafkaMessageParser parser;
	private String topic;

	private int msgLength;
	private ByteBuffer raw;
	private byte[] arr;
	
	public KafkaHistoricalIterator(InputStream inputStream, String topic) throws IOException {
		rawIterator = new RawMessageIterator(inputStream, 4, 65536, ByteOrder.BIG_ENDIAN);
		parser = new KafkaMessageParser();
		this.topic = topic;
	}
	
	@Override
	public boolean hasNext() {
		return rawIterator.hasNext();
	}

	@Override
	public Object next() {
		msgLength = rawIterator.nextMessageLength();
		raw = rawIterator.next();
		if (raw==null)
			return null;

		arr = new byte[msgLength];
		raw.get(arr);
		return parser.parse(UnifinaKafkaMessageFactory.parse(topic, new byte[0], arr));
	}

	@Override
	public void remove() {
		throw new RuntimeException("Remove operation not supported!");
	}

	public void close() throws IOException {
		rawIterator.close();
	}

}