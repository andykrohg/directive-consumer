package com.redhat;

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
		from("kafka:directive")
			.streamCaching()
			.log("Message received from Kafka : ${body}")
			.unmarshal().json(JsonLibrary.Jackson, Map.class)
			.process(new Processor() {
				@Override
				public void process(Exchange exchange) throws Exception {
					Map<String, String> body = exchange.getIn().getBody(Map.class);
					String direction = body.get("direction");
					String username = body.get("username");
					log.info("{}: {}", username, direction);
					Runtime.getRuntime().exec("/Users/akrohg/projects/cliclick/cliclick kd:arrow-" + direction + " w:250 ku:arrow-" + direction);
				}
			});
		    
	}
}
