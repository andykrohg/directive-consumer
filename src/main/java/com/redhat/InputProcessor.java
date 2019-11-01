package com.redhat;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class InputProcessor implements Processor {
	public static long time = System.currentTimeMillis();
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_RESET = "\u001B[0m";

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
		String direction = body.get("direction");
		String username = body.get("username");

		String colorTag = color.equals("red") ? ANSI_RED + "[Team Red Hat] " + ANSI_RESET : ANSI_WHITE + "[Team White Hat] " + ANSI_RESET;
		System.out.println(colorTag + username + ": " + direction);
		StaticServer.eb.publish("log.output", colorTag + username + ": " + direction);
		
		inputs.add(body);
	}
}
