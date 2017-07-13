package fr.umlv.papayaDB.apiServer;

import static java.util.stream.Collectors.joining;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.umlv.papayaDB.databaseManagement.DatabaseManager;
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
	private final DatabaseManager databaseManager = new DatabaseManager();

	/**
	 * demarrer les serveurs HTTP sur le port 8080 et HTTPS sur le port 8070
	 */
	@Override
	public void start() {

		Router routerHttp = Router.router(vertx);
		Router routerHttps = Router.router(vertx);

		routerHttp.route("/*").handler(BodyHandler.create());
		routerHttps.route("/*").handler(BodyHandler.create());

		routerHttp.get("/all").handler(this::getAllDatabases);
		routerHttp.get("/get/:databaseName").handler(this::getDocumentByCriteria);
		routerHttps.get("/getdatabase/:databaseName").handler(this::getDatabase);
		routerHttps.post("/createdatabase/:databaseName").handler(this::createDatabase);
		routerHttp.put("/insertdocument/:databaseName").handler(this::insertDocumentIntoDatabase);
		routerHttp.put("/uploadfile/:databaseName").handler(this::uploadFile);
		routerHttp.delete("/drop").handler(this::dropDocumentByName);
		routerHttps.delete("/dropdatabase/:databaseName").handler(this::dropDatabase);

		routerHttp.route().handler(StaticHandler.create());
		routerHttps.route().handler(StaticHandler.create());

		vertx.createHttpServer().requestHandler(routerHttp::accept).listen(8080);
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

	private void uploadFile(RoutingContext routingContext) {
		routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
				.end(databaseManager
						.uploadFile(routingContext.request().getParam("databaseName"), routingContext.getBodyAsString())
						.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
	}

	private void insertDocumentIntoDatabase(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(201)
					.putHeader("content-type", "application/json").end(
							databaseManager
									.insertDocumentIntoDatabase(routingContext.request().getParam("databaseName"),
											(ObjectNode) new ObjectMapper()
													.readTree(routingContext.getBodyAsJson().toString()))
									.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void createDatabase(RoutingContext routingContext) {
		if (isAuthentified(routingContext.request())) {
			try {
				routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
						.end(databaseManager.createDatabase(routingContext.request().getParam("databaseName"))
								.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
			} catch (IllegalStateException ise) {
				routingContext.response().setStatusCode(400).putHeader("content-type", "application/json").end();
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
							.getDocumentByCriteria(routingContext.request().getParam("databaseName"),
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
							.dropDocumentByName(routingContext.request().getParam("databaseName"),
									routingContext.getBodyAsString())
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void getDatabase(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
					.end(databaseManager.getDatabaseDocuments(routingContext.request().getParam("databaseName"))
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {

			routingContext.response().setStatusCode(404).end();
		}
	}

	private void dropDatabase(RoutingContext routingContext) {
		if (isAuthentified(routingContext.request())) {
			try {
				routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
						.end(databaseManager.dropDatabase(routingContext.request().getParam("databaseName"))
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
