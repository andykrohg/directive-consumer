package com.redhat;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

@Component
public class StaticServer extends AbstractVerticle {

	@Autowired
	AppConfiguration configuration;
	public static EventBus eb;

	@Override
	public void start() throws Exception {
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
		vertx.createHttpServer().requestHandler(router).listen(configuration.httpPort());

		eb = vertx.eventBus();
//		eb.publish("log.output", "Hi, I'm awesome");
	}
}