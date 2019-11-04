package com.redhat;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import com.sun.org.apache.xpath.internal.Arg;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;


public class Server extends AbstractVerticle {
	public static void main(String[] args) {
		Vertx.vertx().deployVerticle(Server.class.getName());
	}
	
	public static EventBus eb;
	private ConsumerRoute consumerRoute = new ConsumerRoute();

	@Override
	public void start() throws Exception {
		// Start Camel
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(consumerRoute);
        context.start();
        
		Router router = Router.router(vertx);

		// Serve the static pages
		router.route().handler(StaticHandler.create());

		// Allow events for the designated addresses in/out of the event bus bridge
		BridgeOptions opts = new BridgeOptions()
				.addOutboundPermitted(new PermittedOptions().setAddress("log.output"))
				.addOutboundPermitted(new PermittedOptions().setAddress("red.move"))
				.addOutboundPermitted(new PermittedOptions().setAddress("white.move"));

		// Create the event bus bridge and add it to the router.
		SockJSHandler ebHandler = SockJSHandler.create(vertx);
		ebHandler.bridge(opts);
		router.route("/eventbus/*").handler(ebHandler);

		// Start the web server and tell it to use the router to handle requests.
		vertx.createHttpServer().requestHandler(router).listen(8080);

		eb = vertx.eventBus();
		
		router.get("/start").handler(requestHandler -> {
			try {
				consumerRoute.startGame();
				requestHandler.response().end();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		router.get("/join-link").handler(requestHandler -> {
			requestHandler.response().end(System.getenv("JOIN_LINK"));
		});
		
		router.get("/camel/rest/gameover/:color").handler(requestHandler -> {
			String color = requestHandler.request().getParam("color");
			consumerRoute.gameOver(color);
			requestHandler.response().end();
		});
	}
}