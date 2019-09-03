package com.redhat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class ConsumerRoute extends RouteBuilder {
	protected long time = System.currentTimeMillis();
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_RESET = "\u001B[0m";
//	protected Map<String, Integer> redInputs = new HashMap<String, Integer>() {{
//		put("left", 0);
//		put("right", 0);
//		put("up", 0);
//		put("down", 0);
//	}};
//	protected Map<String, Integer> whiteInputs = new HashMap<String, Integer>() {{
//		put("left", 0);
//		put("right", 0);
//		put("up", 0);
//		put("down", 0);
//	}};
	protected List<Map<String, String>> redInputs = new ArrayList<Map<String, String>>();
	protected List<Map<String, String>> whiteInputs = new ArrayList<Map<String, String>>();
	
	protected Map<String, Map<Boolean, Integer>> redUserData = new HashMap<String, Map<Boolean, Integer>>();
	protected Map<String, Map<Boolean, Integer>> whiteUserData = new HashMap<String, Map<Boolean, Integer>>();
	
	
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet").bindingMode(RestBindingMode.json);
		
		KafkaComponent kafka = new KafkaComponent();		
		KafkaConfiguration kafkaConfig = new KafkaConfiguration();
		kafkaConfig.setBrokers("my-cluster-kafka-bootstrap-myproject.apps.akrohg-openshift.redhatgov.io:443");
		kafkaConfig.setSecurityProtocol("SSL");
		kafkaConfig.setSslTruststoreLocation("src/main/resources/keystore.jks");
		kafkaConfig.setSslTruststorePassword("password");
		kafka.setConfiguration(kafkaConfig);
		
		getContext().addComponent("kafka", kafka);
		
		// Consumer for messages which correspond to the "directive" kafka topic
		from("kafka:directive-red?synchronous=true")
			.streamCaching()
			.unmarshal().json(JsonLibrary.Jackson, Map.class)
			.process(new DirectiveProcessor(redUserData, redInputs, "red"));    
		
		from("kafka:directive-white?synchronous=true")
		.streamCaching()
		.unmarshal().json(JsonLibrary.Jackson, Map.class)
		.process(new DirectiveProcessor(whiteUserData, whiteInputs, "white")); 
	}
	
	private class DirectiveProcessor implements Processor {
		Map<String, Map<Boolean, Integer>> userData;
		List<Map<String, String>> inputs;
		String color;	
		
		public DirectiveProcessor(Map<String, Map<Boolean, Integer>> userData, List<Map<String, String>> inputs, String color) {
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
			
			final String consensus = inputs.stream().collect(Collectors.groupingBy(map -> map.get("direction"), Collectors.counting()))
					.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
							
			System.out.println("Consensus:" + consensus);
			
			inputs.parallelStream().forEach(input -> {
				boolean agreement = input.get("direction").equals(consensus);
				
				userData.merge(input.get("username"), new HashMap<Boolean, Integer>(){{put(agreement, 1);put(!agreement,0);}}, (user1, user2) -> {
					return new HashMap<Boolean, Integer> () {{
						put(agreement, user1.get(agreement) + 1);
						put(!agreement, user1.get(!agreement));
					}};
				});
			});
			
			
			//Who has agreed with the consensus the most?
			String goodGuy = userData.keySet().stream().max(new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					// TODO Auto-generated method stub
					return (Integer.valueOf((userData.get(o1).get(true) - userData.get(o1).get(false)))
							.compareTo(Integer.valueOf(userData.get(o2).get(true) - userData.get(o2).get(false))));
				}
			}).get();
			
			//Who has *disagreed* with the consensus the most?
			String badGuy = userData.keySet().stream().max(new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					// TODO Auto-generated method stub
					return (Integer.valueOf((userData.get(o1).get(false) - userData.get(o1).get(true)))
							.compareTo(Integer.valueOf(userData.get(o2).get(false) - userData.get(o2).get(true))));				}
			}).get();
			
			inputs.clear();
			
			userData.forEach((key,value) -> {
				System.out.print(key + ": ");
				value.forEach((k,v) -> {
					System.out.print(k + "-" + v + " ");
				});
				System.out.println();
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
			
			Process process = Runtime.getRuntime().exec("/Users/akrohg/projects/cliclick/cliclick kd:" + key + " w:250 ku:" + key);
			process.waitFor();
			time = System.currentTimeMillis();
		}
	}
}
