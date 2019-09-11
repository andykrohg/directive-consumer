package com.redhat;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.infinispan.client.hotrod.RemoteCache;
import org.kie.api.runtime.KieSession;

public class DirectiveProcessor implements Processor {
	public static long time = System.currentTimeMillis();
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_RESET = "\u001B[0m";

	RemoteCache<String, Integer> userData;
	List<Map<String, String>> inputs;
	String color;
	KieSession kieSession;

	public DirectiveProcessor(RemoteCache<String, Integer> userData, List<Map<String, String>> inputs, String color, KieSession kieSession) {
		super();
		this.kieSession = kieSession;
		this.userData = userData;
		this.inputs = inputs;
		this.color = color;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		Map<String, String> body = exchange.getIn().getBody(Map.class);
		String direction = body.get("direction");
		String username = body.get("username");

		String colorTag = color.equals("red") ? ANSI_RED + "[Team Red Hat] " + ANSI_RESET : "[Team White Hat] ";
		System.out.println(colorTag + username + ": " + direction);

		inputs.add(body);

		if (System.currentTimeMillis() < time + 25) {
			return;
		}

		final Map<String, Long> totals = inputs.stream()
				.collect(Collectors.groupingBy(map -> map.get("direction").toString(), Collectors.counting()));
		final String consensus = determineConsensus();

		System.out.println("Consensus:" + consensus);

		// +1 for consent, -1 for dissent
		inputs.parallelStream().forEach(input -> {
			int scoreChange = input.get("direction").equals(consensus) ? 1 : -1;
			int score = userData.containsKey(input.get("username")) ? userData.get(input.get("username")) + scoreChange
					: scoreChange;

			// push to Data Grid
			userData.putAsync(input.get("username"), score);
		});

		// Who has agreed with the consensus the most?
		String goodGuy = userData.keySet().stream().max(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return userData.get(o1).compareTo(userData.get(o2));
			}
		}).get();

		// Who has *disagreed* with the consensus the most?
		String badGuy = userData.keySet().stream().min(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return userData.get(o1).compareTo(userData.get(o2));
			}
		}).get();

		// reset the buffer
		inputs.clear();

		userData.forEach((key, value) -> {
			System.out.println(key + ": " + value);
		});

		System.out.println("Good guy: " + goodGuy);
		System.err.println("Bad guy: " + badGuy);

		String key = "";
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
			key = "arrow-" + consensus;
		}

		Process process = Runtime.getRuntime().exec("src/main/resources/cliclick kd:" + key + " w:125 ku:" + key);
		process.waitFor();
		time = System.currentTimeMillis();
	}

	public String determineConsensus() {
		kieSession.insert(inputs);
		kieSession.fireAllRules();
		return kieSession.getGlobal("direction").toString();
	}
}