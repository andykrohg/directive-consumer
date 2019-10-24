package com.redhat;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.infinispan.client.hotrod.RemoteCache;
import org.kie.api.runtime.KieSession;
import org.openqa.selenium.Keys;

public class DirectiveProcessor implements Processor {
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_RESET = "\u001B[0m";

	RemoteCache<String, Integer> userData;
	ArrayBlockingQueue<Map<String, String>> inputs;
	String color;
	KieSession kieSession;

	public DirectiveProcessor(RemoteCache<String, Integer> userData, ArrayBlockingQueue<Map<String, String>> inputs, String color, KieSession kieSession) {
		super();
		this.kieSession = kieSession;
		this.userData = userData;
		this.inputs = inputs;
		this.color = color;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		if (ConsumerRoute.gameOver || inputs.isEmpty()) {
			return;
		}

		ArrayList<Map<String,String>> buffer = new ArrayList<Map<String, String>>();
		inputs.drainTo(buffer);
		
		final Map<String, Long> totals = buffer.parallelStream()
				.collect(Collectors.groupingBy(map -> map.get("direction").toString(), Collectors.counting()));
		
		final String consensus = determineConsensus(totals);
		
		CharSequence key = "";
		if (color.equals("white")) {
			switch (consensus) {
			case "left":
				key = "a";
				break;
			case "right":
				key = "d";
				break;
			case "up":
				key = "w";
				break;
			case "down":
				key = "s";
				break;
			}
		} else {
			switch (consensus) {
			case "left":
				key = Keys.LEFT;
				break;
			case "right":
				key = Keys.RIGHT;
				break;
			case "up":
				key = Keys.UP;
				break;
			case "down":
				key = Keys.DOWN;
				break;
			}
		}

		ConsumerRoute.bodyElement.sendKeys(new String(new char[500]).replace("\0", key));
				
		// +1 for consent, -1 for dissent
		buffer.parallelStream().forEach(input -> {
			int scoreChange = input.get("direction").equals(consensus) ? 1 : -1;
			int score = userData.containsKey(input.get("username")) ? userData.get(input.get("username")) + scoreChange
					: scoreChange;

			// push to Data Grid
			userData.putAsync(input.get("username"), score);
		});
	}

	public String determineConsensus(Map<String, Long> totals) {
		kieSession.insert(totals);
		kieSession.fireAllRules();
		return kieSession.getGlobal("direction").toString();
	}
}