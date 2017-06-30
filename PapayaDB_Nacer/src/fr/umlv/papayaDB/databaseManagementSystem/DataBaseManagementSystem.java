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

public class DataBaseManagementSystem implements Queryable{
	final static String PATH_OF_DATABASE_DIRECTORY = "./Database/";
	final static int DATABASE_KEEP_ALIVE_IN_MEMORY = 60000;
	private final HashMap<String,DataBaseInMemory> databasesInMemory = new HashMap<>();
	
	private class DataBaseInMemory{
		private DataBase db;
		private Thread thread = deleteAtEndOfDelay();
		
		DataBaseInMemory(String nameOfDatabase) throws IOException{
			this.db = new DataBase(nameOfDatabase);
			thread.start();
		}
		
		DataBase useDB(){
			reloadDelay();
			return db;
		}
		
		void reloadDelay(){
			thread.interrupt();
		}
		
		private Thread deleteAtEndOfDelay(){
			return new Thread(()->{
				for(boolean done = true; done == true;){
					try {
						done = false;
						Thread.sleep(DATABASE_KEEP_ALIVE_IN_MEMORY);
					} catch (InterruptedException e) {
						done = true;
					}
				}
				try {
					unloadDBInMemory(db.name);
				} catch (IOException e) {
					return;
				}
			});
		}
	}
	
	/**
	 * Give the names of all databases on this system
	 * @return a stream with all names of existing databases
	 * @throws IOException if the repository of Databases is not found
	 */
	public Stream<String> getAllDatabases() throws IOException {
		ArrayList<String> allDatabases = new ArrayList<>();
		DirectoryStream<Path> mainDirectory;
		mainDirectory = Files.newDirectoryStream(Paths.get(PATH_OF_DATABASE_DIRECTORY));
		
		mainDirectory.forEach(x->{
			allDatabases.add(x.getFileName().toString());
		});
		
		return allDatabases.stream();
	}
	
	private void loadDBInMemory(String nameOfDatabase) throws IOException{
		if(databasesInMemory.containsKey(nameOfDatabase))
			return;
		
		databasesInMemory.put(nameOfDatabase, new DataBaseInMemory(nameOfDatabase));
	}
	
	private void unloadDBInMemory(String nameOfDatabase) throws IOException{
		if(!databasesInMemory.containsKey(nameOfDatabase))
			return;
		
		DataBaseInMemory dbToDrop = databasesInMemory.remove(nameOfDatabase); // Remove the database of the collection grouping
																	    // databases in memory
		dbToDrop.db.unloadDB(); // Erase all the information of the database
		dbToDrop.db = null; // Erase the database of this Entry
	}

	/**
	 * Drop the database corresponding to the name
	 * @param name = The name of the database
	 * @return a empty Stream
	 * @throws IOException if the database doesn't exist 
	 */
	@Override
	public Stream<Document> dropDatabase(String name) throws IOException {
		synchronized(databasesInMemory){
			unloadDBInMemory(name); // Delete all informations referring to the database in the DataBase object
			dropDBInPhysicalStorage(name); // Delete all files referring to the database on the physical storage
		}
		return Arrays.stream(new Document[0]);
	}
	
	private Stream<Document> dropDBInPhysicalStorage(String nameOfDatabase) throws IOException{
		Path pathDBDirectory = Paths.get(PATH_OF_DATABASE_DIRECTORY + nameOfDatabase + "/");
		DirectoryStream<Path> DBDirectory;
		try {
			DBDirectory = Files.newDirectoryStream(pathDBDirectory);
		} catch (IOException e) {
			return null;
		}
		
		DBDirectory.forEach(x->{x.toFile().delete();});
		DBDirectory.close();
		
		try{
			Files.delete(pathDBDirectory); // Delete the repository of the database 
										   // and all its contents
		} catch (IOException e) {
			return null; // If the repository is not found, it's not necessary to throw an IOException  
		}
		return null;
	}
	
	/**
	 * Create the database called name
	 * @param name = The name of the database
	 * @return a empty Stream
	 * @throws IOException if it is not possible to create the database on the physical storage
	 * @throws IllegalArgumentException if the database already exists
	 */
	@Override
	public Stream<Document> createDatabase(String name) throws IOException {	
		synchronized(databasesInMemory){
			createDBInPhysicalStorage(name); // Create all files referring to the database on the physical storage
			loadDBInMemory(name); // Add the database in the collection grouping databases in memory
		}
		return Arrays.stream(new Document[0]);
	}
	
	private void createDBInPhysicalStorage(String nameOfDatabase) throws IOException{
		if(Paths.get(PATH_OF_DATABASE_DIRECTORY + nameOfDatabase + "/").toFile().exists())
			throw new IllegalArgumentException("La base de données " + nameOfDatabase + " existe déjà");
			
		Files.createDirectories(Paths.get(PATH_OF_DATABASE_DIRECTORY + nameOfDatabase + "/")); // Create the repository of the database
		Files.createFile(Paths.get(PATH_OF_DATABASE_DIRECTORY + nameOfDatabase + "/" + nameOfDatabase + ".db")); // Create the file containing the documents of the database
	}
	
	/**
	 * Return all documents containing into the database
	 * @param name = The name of the database
	 * @return a Stream containing all documents of the database
	 * @throws IOException if the database doesn't exist
	 */
	@Override
	public Stream<Document> getDatabase(String name) throws IOException {
		return getADB(name).docs.stream();
	}
	
	private DataBase getADB(String nameOfTheDatabase) throws IOException{
		if(!databasesInMemory.containsKey(nameOfTheDatabase)){
			databasesInMemory.put(nameOfTheDatabase, new DataBaseInMemory(nameOfTheDatabase));
		}
		return databasesInMemory.get(nameOfTheDatabase).useDB();
	}
	
	/**
	 * Delete documents of the database corresponding to the name of document in the parameter request
	 * @param name = name of the database
	 * @param request = the name of the document to delete
	 * @return a empty Stream
	 * @throws IOException if the database doesn't exist or it'snt possible to drop on the physical storage
	 */
	@Override
	public Stream<Document> dropDocumentByName(String name, String request) throws IOException{
		getADB(name).drop(request);
		return Arrays.stream(new Document[0]);
	}	

	/**
	 * Insert the document in the database
	 * @param name = name of the database
	 * @param body = a JsonObject in String containing all the fields and associated values of the new document
	 * @return a empty Stream
	 * @throws IOException if the database doesn't exist or it'snt possible to create on the physical storage
	 */
	@Override
	public Stream<Document> insertDocumentIntoDatabase(String name, JsonObject body) throws IOException{
		getADB(name).create(body);
		return Arrays.stream(new Document[0]);
	}
	
	/**
	 * Get documents corresponding to the request in the database
	 * @param name = name of the database
	 * @param criteria = The criteria of selecting documents
	 * @return a Stream containing all documents corresponding to the request in the database
	 * @throws IOException if the database doesn't exist
	 */
	@Override
	public Stream<Document> getDocumentByCriteria(String name, String criteria) throws IOException{
		return getADB(name).get(criteria);
	}
}
