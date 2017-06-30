package fr.umlv.papayaDB.apiServer;

import io.vertx.core.Vertx;

public class MainServer {

	public static void main(String[] args) {

		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new ApiServer());
	}
}
