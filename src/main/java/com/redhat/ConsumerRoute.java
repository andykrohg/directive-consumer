package com.redhat;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.kie.api.runtime.KieSession;

import com.redhat.dm.DroolsBeanFactory;

public class ConsumerRoute extends RouteBuilder {
	protected ArrayBlockingQueue<Map<String, String>> redInputs = new ArrayBlockingQueue<Map<String, String>>(1000);
	protected ArrayBlockingQueue<Map<String, String>> whiteInputs = new ArrayBlockingQueue<Map<String, String>>(1000);
	
	protected RemoteCache<String, Integer> redUserData;
	protected RemoteCache<String, Integer> whiteUserData;
	
	protected KieSession redKieSession;
	protected KieSession whiteKieSession;
	
	protected static boolean gameOver = true;
	
	@Override
	public void configure() throws Exception {
		redKieSession = new DroolsBeanFactory().getKieSession();
		whiteKieSession = new DroolsBeanFactory().getKieSession();
		
		Properties props = new Properties();
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("kafka.properties"));
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("datagrid.properties"));
		
		//configure Kafka
		TrustStore.createFromCrtFile("/tmp/certs/kafka/ca.crt",
				props.getProperty("kafka.ssl.truststore.location"),
				props.getProperty("kafka.ssl.truststore.password").toCharArray());
		
		KafkaComponent kafka = new KafkaComponent();		
		KafkaConfiguration kafkaConfig = new KafkaConfiguration();
		kafkaConfig.setBrokers(System.getenv("KAFKA_BROKERS"));
		kafkaConfig.setSecurityProtocol(props.getProperty("kafka.security.protocol"));
		kafkaConfig.setSslTruststoreLocation(props.getProperty("kafka.ssl.truststore.location"));
		kafkaConfig.setSslTruststorePassword(props.getProperty("kafka.ssl.truststore.password"));
		kafka.setConfiguration(kafkaConfig);
		
		getContext().addComponent("kafka", kafka);
		
		//configure Data Grid
		props.put("infinispan.client.hotrod.server_list", System.getenv("DATAGRID_HOST") + ":" + System.getenv("DATAGRID_PORT"));
		props.put("infinispan.client.hotrod.sni_host_name", System.getenv("DATAGRID_HOST"));
		props.put("infinispan.client.hotrod.auth_username", System.getenv("DATAGRID_USERNAME"));
		props.put("infinispan.client.hotrod.auth_password", System.getenv("DATAGRID_PASSWORD"));
		
		String template = null;
		Configuration config = new ConfigurationBuilder().withProperties(props).build();
		RemoteCacheManager manager = new RemoteCacheManager(config);
		redUserData = manager.administration().getOrCreateCache("red-data", template);
		whiteUserData = manager.administration().getOrCreateCache("white-data", template);
		
		
		from("kafka:directive-red")
			.id("red")
			.streamCaching()
			.unmarshal().json(JsonLibrary.Jackson, Map.class).process(new InputProcessor(redInputs, "red"));
			    
		
		from("kafka:directive-white")
			.id("white")
			.streamCaching()
			.unmarshal().json(JsonLibrary.Jackson, Map.class)
			.process(new InputProcessor(whiteInputs, "white"));
		
		from("timer://move?fixedRate=true&period=500")
			.multicast().to("direct:red-processor", "direct:white-processor");
		
		from("direct:red-processor")
			.process(new DirectiveProcessor(redUserData, redInputs, "red", redKieSession));
		from("direct:white-processor")
			.process(new DirectiveProcessor(whiteUserData, whiteInputs, "white", whiteKieSession));
	}
	
	public void startGame() throws Exception {
		redUserData.clear();
		whiteUserData.clear();
		gameOver = false;
	}
	
	public void gameOver(String winningColor) {
		if (gameOver) {
			return;
		}
		gameOver = true;

		String message = winningColor.equals("red") ? 
				colorize("TEAM RED HAT WINS!!!", "red") :
					colorize("TEAM WHITE HAT WINS!!!", "white") ;

	    //Display winner banner
		message += "<br/><br/>";
		
		//Display Red Team data
		message += colorize("Team Red Hat Data<br/>", "red");
		message += "MVP: " + findMVP(redUserData) +" <br/>";
		message += "Biggest Troll: " + findTroll(redUserData) + "<br/><br/>";
		
		//Display White Team data
		message += colorize("Team White Hat Data<br/>", "white");
		message += "MVP: " + findMVP(whiteUserData) + "<br/>";
		message += "Biggest Troll: " + findTroll(whiteUserData) + "<br/><br/>";
		
		message += "Press ESC to play again!";
		
		Server.eb.publish("game.over", message);
		System.out.println(message);
	}
	
	private String findMVP(Map<String, Integer> userData) {
		return userData.isEmpty() ? "No one" : userData.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
	}
	
	private String findTroll(Map<String, Integer> userData) {
		return userData.isEmpty() ? "No one" : userData.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey();
	}
	
	public static String colorize(String content, String color) {
		return "<span class='font-" + color + "'>" + content + "</span>";
	}
}
