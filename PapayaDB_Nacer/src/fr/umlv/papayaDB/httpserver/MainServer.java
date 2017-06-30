package fr.umlv.papayaDB.httpserver;

import java.net.URISyntaxException;

import io.vertx.core.Vertx;

public class MainServer {

	public static void main(String[] args) {
		
			try {
				Vertx vertx = Vertx.vertx();
				vertx.deployVerticle(new HttpQuery("localhost"));
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("address of the REST server is not valid.");
			}
		
	}
}
