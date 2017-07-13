package fr.umlv.papayaDB.apiClient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.umlv.papayaDB.utils.Decoder;
import fr.umlv.papayaDB.utils.PapayaUtils;
import io.vertx.core.json.Json;

/**
 * @author JLILI Mohamed Kacem & REZGUI Ichrak
 *
 */
public class ApiClient {
	private final URI httpUri;
	private final URI httpsUri;
	static final String SERVER_ADDRESS = "localhost";

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DatabaseQuery {
		String value() default "";
	}

	/**
	 * Constructeur de l'API Cliente
	 * 
	 * @param uri
	 *            Adresse du serveur REST a requeter
	 * @throws URISyntaxException
	 *             si l'URI est mal forme
	 */
	public ApiClient() throws URISyntaxException {
		this.httpUri = new URI("http://" + SERVER_ADDRESS + ":8080");
		this.httpsUri = new URI("https://" + SERVER_ADDRESS + ":8070");
	}

	/**
	 * Envoi une requete HTTP au serveur REST pour obtenir toute les DBs
	 * 
	 * @return String renvoit le nom de toutes les BDDs
	 */
	@DatabaseQuery("GET ALL DATABASES")
	public String getAllDatabases() {
		HttpResponse response;
		try {
			response = HttpClient.getDefault().request(httpUri.resolve("/all"))
					.headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close").GET().response();
		} catch (IOException | InterruptedException e) {
			return "request failed";
		}
		if (response.statusCode() == 200) {
			return response.body(HttpResponse.asString());
		}
		return "FAILED : No databases to display";
	}

	@DatabaseQuery("UPLOAD FILE")
	public String uploadFileContent(String databaseName, String filePath) {
		// upload file -> test testJSON.json
		List<ObjectNode> jsonObjects;
		try {
			jsonObjects = PapayaUtils.extractJsonObjectsFromFile(filePath);
		} catch (JsonParseException e1) {
			return "Json File content is currupted";
		} catch (IOException e1) {
			e1.printStackTrace();
			return "Internal error reading input file";
		}
		HttpResponse response;
		try {
			response = HttpClient.getDefault().request(httpUri.resolve("/uploadfile/" + databaseName))
					.headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close")
					.body(HttpRequest
							.fromString(jsonObjects.stream().map(Json::encodePrettily).collect(Collectors.joining())))
					.PUT().response();
			if (response.statusCode() == 201) {
				return "SUCCESS : File content is uploaded successfully on database " + databaseName;
			}
			return "failed";
		} catch (IOException | InterruptedException e) {
			return "Request failed";
		}
	}

	/**
	 * Envoi une requete HTTP au serveur REST pour ajouter un document a une BDD
	 * 
	 * @param databaseName
	 *            nom de la BDD dans laquelle on insert le document
	 * @param body
	 *            le document a inserer au format Json
	 * @return String retourne "success" si la requete a fonctionnee, "failed"
	 *         si elle a echouee ou "Request failed" si la requete n'a pas pu
	 *         etre effectuee.
	 */
	@DatabaseQuery("INSERT DOCUMENT")
	public String insertDocumentIntoDatabase(String databaseName, String body) {
		// INSERT DOCUMENT -> test {"sname_doc":"test","idate_of_creating":"50"}
		HttpResponse response;
		try {
			response = HttpClient.getDefault().request(httpUri.resolve("/insertdocument/" + databaseName))
					.headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close")
					.body(HttpRequest.fromString(body)).PUT().response();
			if (response.statusCode() == 201) {
				return "success";
			}
			System.err.println("FAILED :" + response.statusCode());
			return "failed";
		} catch (IOException | InterruptedException e) {
			return "Request failed";
		}

	}

	/**
	 * Envoi une requete HTTPS au serveur REST pour creer une BDD
	 * 
	 * @param databaseName
	 *            nom de la BDD a creer
	 * @param logPass
	 *            couple login:password pour se connecter au gestionnaire de BDD
	 * @return String retourne "success" si la requete a fonctionnee, "failed"
	 *         si elle a echouee ou "Request failed" si la requete n'a pas pu
	 *         etre effectuee.
	 */
	@DatabaseQuery("CREATE DATABASE")
	public String createDatabase(String databaseName, String logPass) {
		HttpResponse response;
		try {
			response = HttpClient.getDefault().request(httpsUri.resolve("/createdatabase/" + databaseName))
					.headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close", "Authorization",
							"Basic " + Decoder.encode(logPass))
					.POST().response();
			if (response.statusCode() == 201) {
				return "SUCCESS : The database " + databaseName + " is created";
			} else if (response.statusCode() == 400) {
				return "FAILED : The database " + databaseName + " already exists";
			}
			return "FAILED : Server internal error";
		} catch (IOException | InterruptedException e) {
			if (e.getMessage().equals("Invalid auth header")) {
				return "FAILED : You are not authorized to create a database";
			}
			return e.getMessage();
		}
	}

	static {
		// for localhost testing only
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {

			public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
				if (hostname.equals("localhost")) {
					return true;
				}
				return false;
			}
		});
	}

	/**
	 * Envoi une requete HTTPS au serveur REST pour supprimer une BDD
	 * 
	 * @param databaseName
	 *            nom de la BDD a supprimer
	 * @param logPass
	 *            couple login:password pour se connecter au gestionnaire de BDD
	 * @return String retourne "success" si la requete a fonctionnee, "failed"
	 *         si elle a echouee ou "Request failed" si la requete n'a pas pu
	 *         etre effectuee.
	 */
	@DatabaseQuery("DROP DATABASE")
	public String dropDatabase(String databaseName, String logPass) {
		try {
			HttpsURLConnection httpsConnection = (HttpsURLConnection) httpsUri.resolve("/dropdatabase/" + databaseName)
					.toURL().openConnection();
			httpsConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			httpsConnection.setRequestProperty("Connection", "Close");
			httpsConnection.setRequestProperty("Authorization", "Basic " + Decoder.encode(logPass));
			httpsConnection.setRequestMethod("DELETE");
			httpsConnection.setDoOutput(true);
			if (httpsConnection.getResponseCode() == 200) {
				httpsConnection.disconnect();
				return "SUCCESS : the database " + databaseName + " is dropped";
			}
			httpsConnection.disconnect();
			return "FAILED : " + httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage();
		} catch (IOException e) {
			e.printStackTrace();
			return "request failed";
		}
	}

	/**
	 * Envoi une requete HTTPS au serveur REST pour recuperer une BDD
	 * 
	 * @param databaseName
	 *            nom de la BDD a recuperer
	 * @return String retourne la BDD si la requete a fonctionnee, "failed" si
	 *         elle a echouee ou "Request failed" si la requete n'a pas pu etre
	 *         effectuee.
	 */
	@DatabaseQuery("GET DATABASE")
	public String getDatabase(String databaseName) {
		HttpResponse response;
		try {
			response = HttpClient.getDefault().request(httpsUri.resolve("/getdatabase/" + databaseName))
					.headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close").GET().response();
			if (response.statusCode() == 200) {
				return response.body(HttpResponse.asString());
			}
			return "failed";
		} catch (IOException | InterruptedException e) {
			return "request failed";
		}

	}

	/**
	 * Envoi une requete HTTP au serveur REST pour supprimer des documents
	 * 
	 * @param name
	 *            nom de la BDD dans laquelle on supprime
	 * @param criteria
	 *            critere qui permet de selectionner les documents a supprimer
	 * @return String retourne "success" si la requete a fonctionnee, "failed"
	 *         si elle a echouee ou "Request failed" si la requete n'a pas pu
	 *         etre effectuee.
	 */
	@DatabaseQuery("DROP DOCUMENT")
	public String dropDocumentByName(String name, String criteria) {
		try {
			HttpURLConnection httpCon = (HttpURLConnection) httpUri.resolve("/drop/" + name).toURL().openConnection();
			httpCon.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			httpCon.setRequestProperty("Connection", "Close");
			httpCon.setRequestMethod("DELETE");
			httpCon.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(httpCon.getOutputStream());
			wr.writeBytes(criteria);
			wr.flush();
			wr.close();
			if (httpCon.getResponseCode() == 200) {
				httpCon.disconnect();
				return "success";
			}
			httpCon.disconnect();
			return "failed";
		} catch (IOException e) {
			return "request failed";
		}
	}

	/**
	 * Envoi une requete HTTPS au serveur REST pour recuperer des documents
	 * 
	 * @param name
	 *            nom de la BDD dans laquelle on recupere
	 * @param criteria
	 *            critere qui permet de selectionner les documents a recuperer
	 * @return String retourne les documents si la requete a fonctionnee,
	 *         "failed" si elle a echouee ou "Request failed" si la requete n'a
	 *         pas pu etre effectuee.
	 */
	@DatabaseQuery("GET DOCUMENT")
	public String getDocumentByCriteria(String name, String criteria) {
		HttpResponse response;
		try {
			response = HttpClient.getDefault().request(httpsUri.resolve("/get/" + name))
					.headers("Accept-Language", "en-US,en;q=0.5", "Connection", "Close")
					.body(HttpRequest.fromString(criteria)).GET().response();
			if (response.statusCode() == 200) {
				return response.body(HttpResponse.asString());
			}
			return "failed";
		} catch (IOException | InterruptedException e) {
			return "request failed";
		}

	}

}
