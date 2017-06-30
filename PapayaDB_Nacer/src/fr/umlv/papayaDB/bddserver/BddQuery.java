package fr.umlv.papayaDB.bddserver;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import fr.umlv.papayaDB.databaseManagementSystem.DataBaseManagementSystem;
import fr.umlv.papayaDB.utils.Decoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * @author DERGAL Nacer LEROUX Gwenael
 */
public class BddQuery extends AbstractVerticle {
	private final DataBaseManagementSystem bdd = new DataBaseManagementSystem();

	/**
	 * demarrer les serveurs HTTP sur le port 8060 et HTTPS sur le port 8050
	 */
	@Override
	public void start() {

		Router router = Router.router(vertx);
		Router routerHttps = Router.router(vertx);

		// route to JSON REST APIs
		router.get("/all").handler(this::getAllDBs);

		router.route("/*").handler(BodyHandler.create());
		routerHttps.route("/*").handler(BodyHandler.create());

		router.put("/insert/:name").handler(this::insert);
		routerHttps.post("/createdatabase/:name").handler(this::createdatabase);
		routerHttps.delete("/dropdatabase/:name").handler(this::dropdatabase);
		routerHttps.get("/getdatabase/:name").handler(this::getdatabase);
		router.get("/get").handler(this::get);
		router.delete("/drop").handler(this::drop);

		// otherwise serve static pages
		router.route().handler(StaticHandler.create());
		routerHttps.route().handler(StaticHandler.create());

		vertx.createHttpServer().requestHandler(router::accept).listen(8060);
		vertx.createHttpServer(createHttpServerOptions()).requestHandler(routerHttps::accept).listen(8050);
		System.out.println(
				"The Server is listening for HTTP requests on port 8060 and for HTTPS requests on port 8050");
	}

	private HttpServerOptions createHttpServerOptions() {
		return new HttpServerOptions().setSsl(true)
				.setKeyStoreOptions(new JksOptions().setPath("keystore.jks").setPassword("changeit"));
	}

	private void getAllDBs(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
					.end(bdd.getAllDBs().map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(404).putHeader("content-type", "application/json").end();
		}
	}

	private void insert(RoutingContext routingContext) {

		try {
			routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
					.end(bdd.create(routingContext.request().getParam("name"), routingContext.getBodyAsJson())
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void createdatabase(RoutingContext routingContext) {
		if (isAuthentified(routingContext.request())) {
			try {
				routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
						.end(bdd.createDB(routingContext.request().getParam("name")).map(Json::encodePrettily)
								.collect(joining(", ", "[", "]")));
			} catch (IllegalArgumentException e) {
				routingContext.response().setStatusCode(201).putHeader("content-type", "application/json").end();
			} catch (IOException e) {
				routingContext.response().setStatusCode(500).putHeader("content-type", "application/json").end();
			}
		} else {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void get(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
					.end(bdd.get(routingContext.request().getParam("name"), routingContext.getBodyAsString())
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void drop(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(201).putHeader("content-type", "application/json")
					.end(bdd.drop(routingContext.request().getParam("name"), routingContext.getBodyAsString())
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(401).end();
		}
	}

	private void getdatabase(RoutingContext routingContext) {
		try {
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
					.end(bdd.getDB(routingContext.request().getParam("name")).map(Json::encodePrettily)
							.collect(joining(", ", "[", "]")));
		} catch (IOException e) {

			routingContext.response().setStatusCode(404).end();
		}
	}

	private void dropdatabase(RoutingContext routingContext) {
		if (isAuthentified(routingContext.request())) {
			try {
				routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
						.end(bdd.dropDB(routingContext.request().getParam("name")).map(Json::encodePrettily)
								.collect(joining(", ", "[", "]")));
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

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new BddQuery());

	}

}
