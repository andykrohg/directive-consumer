package com.redhat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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
	private long time = System.currentTimeMillis();
	private Map<String, Integer> inputs = new HashMap<String, Integer>() {{
		put("left", 0);
		put("right", 0);
		put("up", 0);
		put("down", 0);
	}};
	
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
		from("kafka:directive?synchronous=true")
			.streamCaching()
			.unmarshal().json(JsonLibrary.Jackson, Map.class)
			.process(new Processor() {
				@Override
				public void process(Exchange exchange) throws Exception {
					Map<String, String> body = exchange.getIn().getBody(Map.class);
					String direction = body.get("direction");
					String username = body.get("username");
					System.out.println(username + ": " + direction);
										
					inputs.put(direction, inputs.get(direction) +1);
					
					if (System.currentTimeMillis() < time + 25) {
						return;
					}
					
					//Determine Majority
					direction = inputs.keySet().parallelStream().max(new Comparator<String>() {
						@Override
						public int compare(String o1, String o2) {
							return inputs.get(o1).compareTo(inputs.get(o2));
						}
					}).get();
										
					inputs.keySet().parallelStream().forEach(key -> {
						inputs.put(key, 0);
					});
					
					Process process = Runtime.getRuntime().exec("/Users/akrohg/projects/cliclick/cliclick kd:arrow-" + direction + " w:250 ku:arrow-" + direction);
					process.waitFor();
					time = System.currentTimeMillis();
				}
			});    
	}
}
