package fr.umlv.papayaDB.databaseManagementSystem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;

import fr.umlv.papayaDB.databaseManagementSystem.document.Document;
import io.vertx.core.json.JsonObject;

public class DataBaseManagementSystem implements Queryable {
	final static String PATH_OF_DATABASE_DIRECTORY = "./Database/";
	final static int DATABASE_KEEP_ALIVE_IN_MEMORY = 60000;
	private final HashMap<String, DataBaseInMemory> databasesInMemory = new HashMap<>();

	private class DataBaseInMemory {
		private DataBase database;
		private Thread thread = unloadUnusedDatabase();

		DataBaseInMemory(String nameOfDatabase) throws IOException {
			this.database = new DataBase(nameOfDatabase);
			thread.start();
		}

		DataBase useDatabase() {
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

	private void loadDatabaseInMemory(String nameOfDatabase) throws IOException {
		if (databasesInMemory.containsKey(nameOfDatabase))
			return;

		databasesInMemory.put(nameOfDatabase, new DataBaseInMemory(nameOfDatabase));
	}

	private void unloadDatabaseFromMemory(String nameOfDatabase) throws IOException {
		if (!databasesInMemory.containsKey(nameOfDatabase))
			return;

		DataBaseInMemory dbToDrop = databasesInMemory.remove(nameOfDatabase); // Remove
																				// the
																				// database
																				// of
																				// the
																				// collection
																				// grouping
		// databases in memory
		dbToDrop.database.unloadDB(); // Erase all the information of the
										// database
		dbToDrop.database = null; // Erase the database of this Entry
	}

	/**
	 * Drop the database corresponding to the name
	 * 
	 * @param name
	 *            = The name of the database
	 * @return a empty Stream
	 * @throws IOException
	 *             if the database doesn't exist
	 */
	@Override
	public Stream<Document> dropDatabase(String name) throws IOException {
		synchronized (databasesInMemory) {
			unloadDatabaseFromMemory(name); // Delete all informations referring
											// to the
			// database in the DataBase object
			dropDBInPhysicalStorage(name); // Delete all files referring to the
											// database on the physical storage
		}
		return Arrays.stream(new Document[0]);
	}

	private Stream<Document> dropDBInPhysicalStorage(String nameOfDatabase) throws IOException {
		Path pathDBDirectory = Paths.get(PATH_OF_DATABASE_DIRECTORY + nameOfDatabase + "/");
		DirectoryStream<Path> DBDirectory;
		try {
			DBDirectory = Files.newDirectoryStream(pathDBDirectory);
		} catch (IOException e) {
			return null;
		}

		DBDirectory.forEach(x -> {
			x.toFile().delete();
		});
		DBDirectory.close();

		try {
			Files.delete(pathDBDirectory); // Delete the repository of the
											// database
											// and all its contents
		} catch (IOException e) {
			return null; // If the repository is not found, it's not necessary
							// to throw an IOException
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
			savaeDatabaseOnDisk(databaseName);
			loadDatabaseInMemory(databaseName);
		}
		return Arrays.stream(new Document[0]);
	}

	private void savaeDatabaseOnDisk(String databaseName) throws IOException {
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

	private DataBase getDatabase(String databaseName) throws IOException {
		DataBaseInMemory databaseInMemory = databasesInMemory.get(databaseName);
		if (databaseInMemory == null) {
			databasesInMemory.put(databaseName, new DataBaseInMemory(databaseName));
		}
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
		getDatabase(databaseName).insertDocument(documentContent);
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
}
