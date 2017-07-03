package fr.umlv.papayaDB.databaseManagement;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;

import fr.umlv.papayaDB.databaseManagement.document.Document;
import fr.umlv.papayaDB.utils.PapayaUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DatabaseManager implements Queryable {
	final static String PATH_OF_DATABASE_DIRECTORY = "./Database/";
	final static int DATABASE_KEEP_ALIVE_IN_MEMORY = 60000;
	private final HashMap<String, DatabaseInMemory> databasesInMemory = new HashMap<>();

	private class DatabaseInMemory {
		Database database;
		private Thread thread = unloadUnusedDatabase();

		DatabaseInMemory(String databaseName) throws IOException {
			this.database = new Database(databaseName);
			thread.start();
		}

		Database useDatabase() {
			reloadDelay();
			return database;
		}

		void reloadDelay() {
			thread.interrupt();
		}

		private Thread unloadUnusedDatabase() {
			return new Thread(() -> {
				for (boolean done = true; done == true;) {
					try {
						done = false;
						Thread.sleep(DATABASE_KEEP_ALIVE_IN_MEMORY);
					} catch (InterruptedException e) {
						done = true;
					}
				}
				try {
					unloadDatabaseFromMemory(database.databaseName);
				} catch (IOException e) {
					return;
				}
			});
		}
	}

	/**
	 * Give the names of all databases on this system
	 * 
	 * @return a stream with all names of existing databases
	 * @throws IOException
	 *             if the repository of Databases is not found
	 */
	public Stream<String> getAllDatabases() throws IOException {
		ArrayList<String> databases = new ArrayList<>();
		DirectoryStream<Path> databaseDirectory = Files.newDirectoryStream(Paths.get(PATH_OF_DATABASE_DIRECTORY));

		databaseDirectory.forEach(file -> {
			databases.add(file.getFileName().toString());
		});

		return databases.stream();
	}

	private void loadDatabaseInMemory(String databaseName) throws IOException {
		if (databasesInMemory.containsKey(databaseName)) {
			return;
		}
		databasesInMemory.put(databaseName, new DatabaseInMemory(databaseName));
	}

	private void unloadDatabaseFromMemory(String nameOfDatabase) throws IOException {
		if (!databasesInMemory.containsKey(nameOfDatabase)) {
			return;
		}
		DatabaseInMemory databaseToDrop = databasesInMemory.remove(nameOfDatabase);
		databaseToDrop.database.unloadDatabase();
		databaseToDrop.database = null;
	}

	/**
	 * Drop the database corresponding to the name
	 * 
	 * @param databaseName
	 *            = The name of the database
	 * @return a empty Stream
	 * @throws IOException
	 *             if the database doesn't exist
	 */
	@Override
	public Stream<Document> dropDatabase(String databaseName) throws IOException {
		synchronized (databasesInMemory) {
			if (PapayaUtils.databaseExists(databaseName)) {
				unloadDatabaseFromMemory(databaseName);
				dropDatabaseFromDisk(databaseName);
			} else {
				throw new IllegalStateException("Database not found");
			}
		}
		return Arrays.stream(new Document[0]);
	}

	private Stream<Document> dropDatabaseFromDisk(String nameOfDatabase) throws IOException {
		Path pathDatabaseDirectory = Paths.get(PATH_OF_DATABASE_DIRECTORY + nameOfDatabase + "/");
		DirectoryStream<Path> databaseDirectory;
		try {
			databaseDirectory = Files.newDirectoryStream(pathDatabaseDirectory);
		} catch (IOException e) {
			return null;
		}
		databaseDirectory.forEach(x -> {
			x.toFile().delete();
		});
		databaseDirectory.close();
		try {
			Files.delete(pathDatabaseDirectory);
		} catch (IOException e) {
			return null;
		}
		return null;
	}

	/**
	 * Create the database called name
	 * 
	 * @param databaseName
	 *            = The name of the database
	 * @return a empty Stream
	 * @throws IOException
	 *             if it is not possible to create the database on the physical
	 *             storage
	 * @throws IllegalArgumentException
	 *             if the database already exists
	 */
	@Override
	public Stream<Document> createDatabase(String databaseName) throws IOException {
		synchronized (databasesInMemory) {
			if (!PapayaUtils.databaseExists(databaseName)) {
				saveDatabaseOnDisk(databaseName);
				loadDatabaseInMemory(databaseName);
			} else {
				throw new IllegalStateException("Database is already created");
			}
		}
		return Arrays.stream(new Document[0]);
	}

	private void saveDatabaseOnDisk(String databaseName) throws IOException {
		if (Paths.get(PATH_OF_DATABASE_DIRECTORY + databaseName + "/").toFile().exists())
			throw new IllegalArgumentException("La base de données " + databaseName + " existe déjà");
		Files.createDirectories(Paths.get(PATH_OF_DATABASE_DIRECTORY + databaseName + "/"));
		Files.createFile(Paths.get(PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + databaseName + ".db"));
	}

	/**
	 * Return all documents containing into the database
	 * 
	 * @param databaseName
	 *            = The name of the database
	 * @return a Stream containing all documents of the database
	 * @throws IOException
	 *             if the database doesn't exist
	 */
	@Override
	public Stream<Document> getDatabaseDocuments(String databaseName) throws IOException {
		return getDatabase(databaseName).documents.stream();
	}

	private Database getDatabase(String databaseName) throws IOException {
		DatabaseInMemory databaseInMemory = databasesInMemory.get(databaseName);
		if (databaseInMemory == null) {
			databasesInMemory.put(databaseName, new DatabaseInMemory(databaseName));
		}
		System.out.println("DATABASE IN MEMORY : " + databaseInMemory.database.databaseName);
		return databaseInMemory.useDatabase();
	}

	/**
	 * Delete documents of the database corresponding to the name of document in
	 * the parameter request
	 * 
	 * @param databaseName
	 *            = name of the database
	 * @param criteria
	 *            = the name of the document to delete
	 * @return a empty Stream
	 * @throws IOException
	 *             if the database doesn't exist or it'snt possible to drop on
	 *             the physical storage
	 */
	@Override
	public Stream<Document> dropDocumentByName(String databaseName, String criteria) throws IOException {
		getDatabase(databaseName).drop(criteria);
		return Arrays.stream(new Document[0]);
	}

	/**
	 * Insert the document in the database
	 * 
	 * @param databaseName
	 *            = name of the database
	 * @param documentContent
	 *            = a JsonObject in String containing all the fields and
	 *            associated values of the new document
	 * @return a empty Stream
	 * @throws IOException
	 *             if the database doesn't exist or it'snt possible to create on
	 *             the physical storage
	 */
	@Override
	public Stream<Document> insertDocumentIntoDatabase(String databaseName, JsonObject documentContent)
			throws IOException {
		if (PapayaUtils.databaseExists(databaseName)) {
			getDatabase(databaseName).insertDocument(documentContent);
		} else {
			throw new IllegalArgumentException("Tha requested database does not exist");
		}
		return Arrays.stream(new Document[0]);
	}

	/**
	 * Get documents corresponding to the request in the database
	 * 
	 * @param name
	 *            = name of the database
	 * @param criteria
	 *            = The criteria of selecting documents
	 * @return a Stream containing all documents corresponding to the request in
	 *         the database
	 * @throws IOException
	 *             if the database doesn't exist
	 */
	@Override
	public Stream<Document> getDocumentByCriteria(String name, String criteria) throws IOException {
		return getDatabase(name).get(criteria);
	}

	/**
	 * Inserts the file content into database
	 * 
	 * @param databaseName
	 *            = name of the database
	 * @param jsonObjects
	 *            = a JsonObject Array containing all the documents contained
	 *            into the file
	 * @return a empty Stream
	 * @throws IOException
	 *             if the database doesn't exist or it'snt possible to create on
	 *             the physical storage
	 */
	@Override
	public Stream<Document> uploadFile(String databaseName, JsonArray jsonObjects) {
		Database database;
		try {
			database = getDatabase(databaseName);
		} catch (IOException e) {
			throw new IllegalArgumentException("The requested Database does not exist");
		}
		for (Object jsonObject : jsonObjects) {
			try {
				database.insertDocument((JsonObject) jsonObject);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot write values on database");
			}
		}
		return Arrays.stream(new Document[0]);
	}
}
