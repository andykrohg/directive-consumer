package com.redhat;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class InputProcessor implements Processor {
	ArrayBlockingQueue<Map<String, String>> inputs;
	String color;

	public InputProcessor(ArrayBlockingQueue<Map<String, String>> inputs, String color) {
		super();
		this.inputs = inputs;
		this.color = color;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void process(Exchange exchange) throws Exception {
		if (ConsumerRoute.gameOver) {
			return;
		}
		Map<String, String> body = exchange.getIn().getBody(Map.class);
		
		inputs.add(body);
	}
}
