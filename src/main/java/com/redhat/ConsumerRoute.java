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
import org.apache.camel.component.stream.StreamComponent;
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
	
	protected static boolean gameOver = true;
	
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet").bindingMode(RestBindingMode.json);
		
		Properties props = new Properties();
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("kafka.properties"));
		props.load(ConsumerRoute.class.getClassLoader().getResourceAsStream("datagrid.properties"));
		
		TrustStore.createFromCrtFile("/tmp/certs/ca.crt",
			props.getProperty("kafka.ssl.truststore.location"),
			props.getProperty("kafka.ssl.truststore.password").toCharArray());

		TrustStore.createFromCrtFile("/tmp/certs/tls.crt",
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
		
		
		from("kafka:game-over?synchronous=true")
		.process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				if (gameOver) {
					return;
				}
				gameOver = true;
				
				String winner = exchange.getIn().getBody(String.class);
				String redAscii = "                           ;           \n                           ED.         \n                       ,;  E#Wi        \n  j.                 f#i   E###G.      \n  EW,              .E#t    E#fD#W;     \n  E##j            i#W,     E#t t##L    \n  E###D.         L#D.      E#t  .E#K,  \n  E#jG#W;      :K#Wfff;    E#t    j##f \n  E#t t##f     i##WLLLLt   E#t    :E#K:\n  E#t  :K#E:    .E#L       E#t   t##L  \n  E#KDDDD###i     f#E:     E#t .D#W;   \n  E#f,t#Wi,,,      ,WW;    E#tiW#G.    \n  E#t  ;#W:         .D#;   E#K##i      \n  DWi   ,KK:          tt   E##D.       \n                           E#t         \n                           L:          \n";
				String whiteAscii = "                                                              \n                                                            ,;\n                       .    .       t                     f#i \n            ;          Di   Dt      Ej   GEEEEEEEL      .E#t  \n          .DL          E#i  E#i     E#,  ,;;L#K;;.     i#W,   \n  f.     :K#L     LWL  E#t  E#t     E#t     t#E       L#D.    \n  EW:   ;W##L   .E#f   E#t  E#t     E#t     t#E     :K#Wfff;  \n  E#t  t#KE#L  ,W#;    E########f.  E#t     t#E     i##WLLLLt \n  E#t f#D.L#L t#K:     E#j..K#j...  E#t     t#E      .E#L     \n  E#jG#f  L#LL#G       E#t  E#t     E#t     t#E        f#E:   \n  E###;   L###j        E#t  E#t     E#t     t#E         ,WW;  \n  E#K:    L#W;         f#t  f#t     E#t     t#E          .D#; \n  EG      LE.           ii   ii     E#t      fE            tt \n  ;       ;@                        ,;.       :               \n                                                              ";
				String winsAscii = "                            L.                       .            \n                       t    EW:        ,ft          ;W  ;f.   ;f. \n            ;          Ej   E##;       t#E         f#E  i##:  i##:\n          .DL          E#,  E###t      t#E       .E#f   i##:  i##:\n  f.     :K#L     LWL  E#t  E#fE#f     t#E      iWW;    i##:  i##:\n  EW:   ;W##L   .E#f   E#t  E#t D#G    t#E     L##Lffi  i##:  i##:\n  E#t  t#KE#L  ,W#;    E#t  E#t  f#E.  t#E    tLLG##L   i##:  i##:\n  E#t f#D.L#L t#K:     E#t  E#t   t#K: t#E      ,W#i    i##:  i##:\n  E#jG#f  L#LL#G       E#t  E#t    ;#W,t#E     j#E.     i##:  i##:\n  E###;   L###j        E#t  E#t     :K#D#E   .D#j       i#W.  i#W.\n  E#K:    L#W;         E#t  E#t      .E##E  ,WK,        ,i.   ,i. \n  EG      LE.          E#t  ..         G#E  EG.         :G#:  :G#:\n  ;       ;@           ,;.              fE  ,           iKt   iKt \n                                         ,                        ";
				String message = winner.equals("red") ? 
						DirectiveProcessor.ANSI_RED + redAscii + "\n" + winsAscii + DirectiveProcessor.ANSI_RESET:
							DirectiveProcessor.ANSI_WHITE + whiteAscii + "\n" + winsAscii + DirectiveProcessor.ANSI_RESET;
				
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
}
