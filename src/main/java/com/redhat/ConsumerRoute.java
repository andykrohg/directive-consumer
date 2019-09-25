package com.redhat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Component;

import com.redhat.dm.DroolsBeanFactory;

@Component
public class ConsumerRoute extends RouteBuilder {
	protected List<Map<String, String>> redInputs = new ArrayList<Map<String, String>>();
	protected List<Map<String, String>> whiteInputs = new ArrayList<Map<String, String>>();
	
	protected RemoteCache<String, Integer> redUserData;
	protected RemoteCache<String, Integer> whiteUserData;
	
	protected final KieSession redKieSession = new DroolsBeanFactory().getKieSession();
	protected final KieSession whiteKieSession = new DroolsBeanFactory().getKieSession();
	
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet").bindingMode(RestBindingMode.json);
		
		Properties props = new Properties();
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("kafka.properties"));
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("datagrid.properties"));
		
		String template = null;
		Configuration config = new ConfigurationBuilder().withProperties(props).build();
		RemoteCacheManager manager = new RemoteCacheManager(config);
		redUserData = manager.administration().getOrCreateCache("red-data", template);
		whiteUserData = manager.administration().getOrCreateCache("white-data", template);
		redUserData.clear();
		whiteUserData.clear();
		
		
		KafkaComponent kafka = new KafkaComponent();		
		KafkaConfiguration kafkaConfig = new KafkaConfiguration();
		kafkaConfig.setBrokers(props.getProperty("kafka.brokers"));
		kafkaConfig.setSecurityProtocol(props.getProperty("kafka.security.protocol"));
		kafkaConfig.setSslTruststoreLocation(props.getProperty("kafka.ssl.truststore.location"));
		kafkaConfig.setSslTruststorePassword(props.getProperty("kafka.ssl.truststore.password"));
		kafka.setConfiguration(kafkaConfig);
		
		getContext().addComponent("kafka", kafka);
		
		from("kafka:directive-red?synchronous=true")
		.id("red")
			.streamCaching()
			.unmarshal().json(JsonLibrary.Jackson, Map.class)
			.process(new DirectiveProcessor(redUserData, redInputs, "red", redKieSession));    
		
		from("kafka:directive-white?synchronous=true")
		.id("white")
		.streamCaching()
		.unmarshal().json(JsonLibrary.Jackson, Map.class)
		.process(new DirectiveProcessor(whiteUserData, whiteInputs, "white", whiteKieSession));
		
		from("kafka:game-over")
		.process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				exchange.getContext().stopRoute("red");
				exchange.getContext().stopRoute("white");
				
				String winner = exchange.getIn().getBody(String.class);
				String message = winner.equals("red") ? "Team Red Hat Wins!!" : "Team White Hat Wins!!";
				
				//Clear the console
				System.out.print("\033[H\033[2J");  
			    System.out.flush(); 
		
			    //Display winner banner
				System.out.println(message + "\n\n");
				
				//Display Red Team data
				System.out.println("Team Red Hat Data");
				System.out.println("MVP: " + findMVP(redUserData));
				System.out.println("Biggest Troll: " + findTroll(redUserData) + "\n\n");
				
				//Display White Team data
				System.out.println("Team White Hat Data");
				System.out.println("MVP: " + findMVP(whiteUserData));
				System.out.println("Biggest Troll: " + findTroll(whiteUserData));
				
				manager.stop();
				manager.close();
				redKieSession.dispose();
				whiteKieSession.dispose();
			}
		}).stop();
	}
	
	private String findMVP(Map<String, Integer> userData) {
		return userData.isEmpty() ? "No one" : userData.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
	}
	
	private String findTroll(Map<String, Integer> userData) {
		return userData.isEmpty() ? "No one" : userData.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey();
	}
}
