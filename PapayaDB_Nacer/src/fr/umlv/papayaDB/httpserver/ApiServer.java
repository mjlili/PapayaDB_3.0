package fr.umlv.papayaDB.httpserver;

import static java.util.stream.Collectors.joining;

import java.io.IOException;

import fr.umlv.papayaDB.databaseManagementSystem.DataBaseManagementSystem;
import fr.umlv.papayaDB.utils.Decoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

// java
// --add-exports java.base/sun.nio.ch=ALL-UNNAMED
// --add-exports java.base/sun.net.dns=ALL-UNNAMED
// ExampleApp
/**
 * @author JLILI Mohamed Kacem & REZGUI Ichrak
 *
 */
public class ApiServer extends AbstractVerticle {
	private final DataBaseManagementSystem databaseManager = new DataBaseManagementSystem();

	/**
	 * demarrer les serveurs HTTP sur le port 8080 et HTTPS sur le port 8070
	 */
	@Override
	public void start() {

		Router router = Router.router(vertx);
		Router routerHttps = Router.router(vertx);

		router.route("/*").handler(BodyHandler.create());
		routerHttps.route("/*").handler(BodyHandler.create());
		router.get("/all").handler(this::getAllDatabases);
		router.put("/insert/:name").handler(this::insertDocumentIntoDatabase);
		routerHttps.post("/createdatabase/:name").handler(this::createDatabase);
		routerHttps.delete("/dropdatabase/:name").handler(this::dropDatabase);
		routerHttps.get("/getdatabase/:name").handler(this::getDatabase);
		router.get("/get/:name").handler(this::getDocumentByCriteria);
		router.delete("/drop").handler(this::dropDocumentByName);

		router.route().handler(StaticHandler.create());
		routerHttps.route().handler(StaticHandler.create());

		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
		vertx.createHttpServer(createHttpServerOptions()).requestHandler(routerHttps::accept).listen(8070);
		System.out
				.println("The Server is listening for HTTP requests on port 8080 and for HTTPS requests on port 8070");
	}

	private HttpServerOptions createHttpServerOptions() {
		return new HttpServerOptions().setSsl(true)
				.setKeyStoreOptions(new JksOptions().setPath("keystore.jks").setPassword("changeit"));
	}

	private void getAllDatabases(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
					.end(databaseManager.getAllDatabases().map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(404).putHeader("content-type", "application/json").end();
		}
	}

	private void insertDocumentIntoDatabase(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
					.end(databaseManager
							.insertDocumentIntoDatabase(routingContext.request().getParam("name"),
									routingContext.getBodyAsJson())
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void createDatabase(RoutingContext routingContext) {
		if (isAuthentified(routingContext.request())) {
			try {
				routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
						.end(databaseManager.createDatabase(routingContext.request().getParam("name"))
								.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
			} catch (IllegalArgumentException e) {
				routingContext.response().setStatusCode(201).putHeader("content-type", "application/json").end();
			} catch (IOException e) {
				routingContext.response().setStatusCode(500).putHeader("content-type", "application/json").end();
			}
		} else {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void getDocumentByCriteria(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
					.end(databaseManager
							.getDocumentByCriteria(routingContext.request().getParam("name"),
									routingContext.getBodyAsString())
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void dropDocumentByName(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
					.end(databaseManager
							.dropDocumentByName(routingContext.request().getParam("name"),
									routingContext.getBodyAsString())
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void getDatabase(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
					.end(databaseManager.getDatabase(routingContext.request().getParam("name"))
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {

			routingContext.response().setStatusCode(404).end();
		}
	}

	private void dropDatabase(RoutingContext routingContext) {
		if (isAuthentified(routingContext.request())) {
			try {
				routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
						.end(databaseManager.dropDatabase(routingContext.request().getParam("name"))
								.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
			} catch (IOException e) {
				routingContext.response().setStatusCode(500).putHeader("content-type", "application/json").end();
			}
		} else {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private boolean isAuthentified(HttpServerRequest request) {
		String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);
		if (authorization != null && authorization.substring(0, 6).equals("Basic ")) {
			if (Decoder.decode(authorization.substring(6)).equals("kacem:jlili")) {
				return true;
			}
		}
		return false;
	}
}
