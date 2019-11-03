package com.redhat;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;


public class Server extends AbstractVerticle {
	public static void main(String[] args) {
		Vertx.vertx().deployVerticle(Server.class.getName());
	}
	
	public static EventBus eb;
	public CamelContext context = new DefaultCamelContext();

	@Override
	public void start() throws Exception {
		createCamelRouting();
		Router router = Router.router(vertx);

		// Serve the static pages
		router.route().handler(StaticHandler.create());

		// Allow events for the designated addresses in/out of the event bus bridge
		BridgeOptions opts = new BridgeOptions()
				.addOutboundPermitted(new PermittedOptions().setAddress("log.output"));

		// Create the event bus bridge and add it to the router.
		SockJSHandler ebHandler = SockJSHandler.create(vertx);
		ebHandler.bridge(opts);
		router.route("/eventbus/*").handler(ebHandler);

		// Create a router endpoint for the static content.
		router.route().handler(StaticHandler.create());

		// Start the web server and tell it to use the router to handle requests.
		vertx.createHttpServer().requestHandler(router).listen(8080);

		eb = vertx.eventBus();
		System.out.println("hey");
		System.out.println(context.getRoutes().size());
	}
	
	private void createCamelRouting() throws Exception {
        try {
            context.addRoutes(new ConsumerRoute());
 
            context.start();
 
        } catch (Exception e) {
            throw new Exception(e);
        }
    }
}