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
 * @author DERGAL Nacer LEROUX Gwenael
 *
 */
public class ApiServer extends AbstractVerticle {
	// private final URI httpUri;
	// private final URI httpsUri;
	private final DataBaseManagementSystem databaseManager = new DataBaseManagementSystem();

	// /**
	// * Constructeur de l'API Cliente
	// *
	// * @param uri
	// * Adresse du serveur de BDD a requeter
	// * @throws URISyntaxException
	// * si l'URI est mal forme
	// */
	// public ApiServer(String uri) throws URISyntaxException {
	// Objects.requireNonNull(uri);
	// this.httpUri = new URI("http://" + uri + ":8060");
	// this.httpsUri = new URI("https://" + uri + ":8050");
	// }

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
		// HttpResponse response;
		// try {
		// response = HttpClient.getDefault().request(httpUri.resolve("/all"))
		// .headers("Accept-Language", "en-US,en;q=0.5", "Connection",
		// "Close").GET().response();
		// routingContext.response().setStatusCode(response.statusCode()).putHeader("content-type",
		// "application/json")
		// .end(response.body(HttpResponse.asString()));
		// } catch (IOException | InterruptedException e) {
		// routingContext.response().setStatusCode(401).end();
		// }

		try {
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
					.end(databaseManager.getAllDatabases().map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {
			routingContext.response().setStatusCode(404).putHeader("content-type", "application/json").end();
		}
	}

	private void insertDocumentIntoDatabase(RoutingContext routingContext) {
		// HttpResponse response;
		// try {
		// response = HttpClient.getDefault()
		// .request(httpUri.resolve("/insert/" +
		// routingContext.request().getParam("name")))
		// .headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close")
		// .body(HttpRequest.fromString(routingContext.getBodyAsString())).PUT().response();
		// routingContext.response().setStatusCode(response.statusCode()).putHeader("content-type",
		// "application/json")
		// .end();
		// } catch (IOException | InterruptedException e) {
		// routingContext.response().setStatusCode(401).end();
		// }

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
		// HttpResponse response;
		// try {
		// response = HttpClient.getDefault()
		// .request(httpsUri.resolve("/createdatabase/" +
		// routingContext.request().getParam("name")))
		// .headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close",
		// "Authorization",
		// routingContext.request().headers().get(HttpHeaders.AUTHORIZATION))
		// .POST().response();
		// routingContext.response().setStatusCode(response.statusCode()).end();
		// } catch (IOException | InterruptedException e) {
		// routingContext.response().setStatusCode(401).end();
		// }

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
		// HttpResponse response;
		// try {
		// response = HttpClient.getDefault()
		// .request(httpUri.resolve("/get/" +
		// routingContext.request().getParam("name")))
		// .headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close")
		// .body(HttpRequest.fromString(routingContext.getBodyAsString())).GET().response();
		// routingContext.response().setStatusCode(response.statusCode()).putHeader("content-type",
		// "application/json")
		// .end(response.body(HttpResponse.asString()));
		// } catch (IOException | InterruptedException e) {
		// routingContext.response().setStatusCode(401).end();
		// }
		//
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
		// try {
		// HttpURLConnection httpCon = (HttpURLConnection) httpsUri
		// .resolve("/drop/" +
		// routingContext.request().getParam("name")).toURL().openConnection();
		// httpCon.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		// httpCon.setRequestProperty("Connection", "Close");
		// httpCon.setRequestMethod("DELETE");
		// httpCon.setDoOutput(true);
		// DataOutputStream wr = new
		// DataOutputStream(httpCon.getOutputStream());
		// wr.writeBytes(routingContext.getBodyAsString());
		// wr.flush();
		// wr.close();
		// routingContext.response().setStatusCode(httpCon.getResponseCode())
		// .putHeader("content-type", "application/json").end();
		// httpCon.disconnect();
		// } catch (IOException e) {
		// routingContext.response().setStatusCode(401).putHeader("content-type",
		// "application/json").end();
		// }

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
		// HttpResponse response;
		// try {
		// response = HttpClient.getDefault()
		// .request(httpsUri.resolve("/getdatabase/" +
		// routingContext.request().getParam("name")))
		// .headers("Accept-Language", "en-US,en;q=0.5", "Connection",
		// "Close").GET().response();
		// routingContext.response().setStatusCode(response.statusCode()).putHeader("content-type",
		// "application/json")
		// .end(response.body(HttpResponse.asString()));
		// } catch (IOException | InterruptedException e) {
		// routingContext.response().setStatusCode(401).end();
		// }

		try {
			routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
					.end(databaseManager.getDatabase(routingContext.request().getParam("name"))
							.map(Json::encodePrettily).collect(joining(", ", "[", "]")));
		} catch (IOException e) {

			routingContext.response().setStatusCode(404).end();
		}
	}

	private void dropDatabase(RoutingContext routingContext) {
		// try {
		// HttpsURLConnection httpCon = (HttpsURLConnection) httpsUri
		// .resolve("/dropdatabase/" +
		// routingContext.request().getParam("name")).toURL().openConnection();
		// httpCon.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		// httpCon.setRequestProperty("Connection", "Close");
		// httpCon.setRequestMethod("DELETE");
		// httpCon.setRequestProperty("Authorization",
		// routingContext.request().headers().get(HttpHeaders.AUTHORIZATION));
		// httpCon.setDoOutput(true);
		// routingContext.response().setStatusCode(httpCon.getResponseCode())
		// .putHeader("content-type", "application/json").end();
		// httpCon.disconnect();
		// } catch (IOException e) {
		// routingContext.response().setStatusCode(401).putHeader("content-type",
		// "application/json").end();
		// }
		if (isAuthentified(routingContext.request())) {
			try {
				routingContext.response().setStatusCode(200).putHeader("content-type", "application/json")
						.end(databaseManager.dropDatabase(routingContext.request().getParam("name")).map(Json::encodePrettily)
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
}
