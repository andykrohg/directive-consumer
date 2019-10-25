package com.redhat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.component.stream.StreamComponent;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.commons.io.IOUtils;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.kie.api.runtime.KieSession;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Component;

import com.redhat.dm.DroolsBeanFactory;

@Component
public class ConsumerRoute extends RouteBuilder {
	protected ArrayBlockingQueue<Map<String, String>> redInputs = new ArrayBlockingQueue<Map<String, String>>(1000);
	protected ArrayBlockingQueue<Map<String, String>> whiteInputs = new ArrayBlockingQueue<Map<String, String>>(1000);
	
	protected RemoteCache<String, Integer> redUserData;
	protected RemoteCache<String, Integer> whiteUserData;
	
	protected final KieSession redKieSession = new DroolsBeanFactory().getKieSession();
	protected final KieSession whiteKieSession = new DroolsBeanFactory().getKieSession();
	
	protected static boolean gameOver = true;
	protected static WebDriver driver;
	protected static WebElement bodyElement;
	
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet").bindingMode(RestBindingMode.json);
		
		Properties props = new Properties();
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("kafka.properties"));
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("datagrid.properties"));
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("game.properties"));
		
		copyChromeDriver();
		driver = new ChromeDriver();
		driver.get(props.getProperty("game.url"));
		bodyElement = driver.findElement(By.xpath("/html/body"));
		
		TrustStore.createFromCrtFile("ca.crt",
			props.getProperty("kafka.ssl.truststore.location"),
			props.getProperty("kafka.ssl.truststore.password").toCharArray());

		TrustStore.createFromCrtFile("tls.crt",
			props.getProperty("infinispan.client.hotrod.trust_store_file_name"),
			props.getProperty("infinispan.client.hotrod.trust_store_password").toCharArray());
		
		String template = null;
		Configuration config = new ConfigurationBuilder().withProperties(props).build();
		RemoteCacheManager manager = new RemoteCacheManager(config);
		redUserData = manager.administration().getOrCreateCache("red-data", template);
		whiteUserData = manager.administration().getOrCreateCache("white-data", template);
		
		KafkaComponent kafka = new KafkaComponent();		
		KafkaConfiguration kafkaConfig = new KafkaConfiguration();
		kafkaConfig.setBrokers(props.getProperty("kafka.brokers"));
		kafkaConfig.setSecurityProtocol(props.getProperty("kafka.security.protocol"));
		kafkaConfig.setSslTruststoreLocation(props.getProperty("kafka.ssl.truststore.location"));
		kafkaConfig.setSslTruststorePassword(props.getProperty("kafka.ssl.truststore.password"));
		kafka.setConfiguration(kafkaConfig);
		
		getContext().addComponent("kafka", kafka);
		getContext().addComponent("stream", new StreamComponent());
		
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
		
		from("direct:red-processor").process(new DirectiveProcessor(redUserData, redInputs, "red", redKieSession));
		from("direct:white-processor").process(new DirectiveProcessor(whiteUserData, whiteInputs, "white", whiteKieSession));
		
		from("kafka:game-over?synchronous=true")
		.process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				if (gameOver) {
					return;
				}
				gameOver = true;
				
				String winner = exchange.getIn().getBody(String.class);
				String message = winner.equals("red") ? 
						DirectiveProcessor.ANSI_RED + "TEAM RED HAT WINS!!!" + DirectiveProcessor.ANSI_RESET:
							DirectiveProcessor.ANSI_WHITE + "TEAM WHITE HAT WINS!!!" + DirectiveProcessor.ANSI_RESET;
				
				//Clear the console
				System.out.print("\033[H\033[2J");  
			    System.out.flush(); 
		
			    //Display winner banner
				System.out.println(message + "\n\n");
				
				//Display Red Team data
				System.out.println(DirectiveProcessor.ANSI_RED + "Team Red Hat Data" + DirectiveProcessor.ANSI_RESET);
				System.out.println("MVP: " + findMVP(redUserData));
				System.out.println("Biggest Troll: " + findTroll(redUserData) + "\n\n");
				
				//Display White Team data
				System.out.println(DirectiveProcessor.ANSI_WHITE + "Team White Hat Data" + DirectiveProcessor.ANSI_RESET);
				System.out.println("MVP: " + findMVP(whiteUserData));
				System.out.println("Biggest Troll: " + findTroll(whiteUserData));
				
				System.out.println("\n\nPress ENTER to play again, or Ctrl+C to quit");
			}
		});
		
		from("stream:in")
			.process(new Processor() {
				@Override
				public void process(Exchange exchange) throws Exception {
					if (! gameOver) {
						return;
					}
					
					startGame();
				}
			});
		
		//Clear the console
		System.out.print("\033[H\033[2J");  
	    System.out.flush(); 
	    System.out.println("Press ENTER to start!");
	}
	
	private void startGame() throws Exception {
		System.out.print("\033[H\033[2J");  
	    System.out.flush(); 
	    System.out.println("3...");
	    Thread.sleep(1000);
	    System.out.println("2...");
	    Thread.sleep(1000);
	    System.out.println("1...");
	    Thread.sleep(1000);
		System.out.println("Go!!");
		
		redUserData.clear();
		whiteUserData.clear();
		gameOver = false;
	}
	
	private String findMVP(Map<String, Integer> userData) {
		return userData.isEmpty() ? "No one" : userData.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
	}
	
	private String findTroll(Map<String, Integer> userData) {
		return userData.isEmpty() ? "No one" : userData.entrySet().stream().min(Map.Entry.comparingByValue()).get().getKey();
	}
	
	private void copyChromeDriver() throws FileNotFoundException, IOException {
		String os = System.getProperty("os.name").toLowerCase();
		os = os.contains("mac") ? "osx" : os.contains("nux") ? "linux" : "undefined";
		if ( ! Files.exists(Paths.get("chromedriver"))) {
			IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("chromedriver-" + os), new FileOutputStream("chromedriver"));
		}
		new File("chromedriver").setExecutable(true);
		System.setProperty("webdriver.chrome.driver", "chromedriver");
	}
}
