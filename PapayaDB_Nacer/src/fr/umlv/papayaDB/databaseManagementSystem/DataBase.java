package fr.umlv.papayaDB.databaseManagementSystem;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import fr.umlv.papayaDB.databaseManagementSystem.document.Document;
import fr.umlv.papayaDB.databaseManagementSystem.document.GenericValue;
import fr.umlv.papayaDB.databaseManagementSystem.document.IntegerValue;
import fr.umlv.papayaDB.databaseManagementSystem.document.Parser;
import io.vertx.core.json.JsonObject;

public class DataBase{
	List<Document> docs;
	String name;
	private final HashMap<String, TreeMap<Object, List<Document>>> indexs = new HashMap<>();
	private FileChannel fileDB;
	private RandomAccessFile raf;
	private volatile int nbToDelete = 0;

	/**
	 * Constructor
	 * @param nameOfDatabase = the name of the database
	 * @throws IOException if it'snt possible to load the database
	 */
	public DataBase(String nameOfDatabase) throws IOException {
		this.name = nameOfDatabase;
		docs = loadDB();
		loadIndexs();
		launchThreadDelete();
	}

	private void launchThreadDelete(){
		Thread t = new Thread(()->{
			for(;;){
				synchronized (indexs) {
					while(nbToDelete/((docs.size() == 0)?1:docs.size()) < 0.8){
						try {
							indexs.wait();
						} catch (InterruptedException e) {}
					}
					try {
						createNewDatabase();
						nbToDelete = 0;
					} catch (IOException e) {}
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	
	private void createNewDatabase() throws IOException{
		synchronized (indexs){
			Files.createFile(Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory + name + "/" 
					+ name + ".dbtmp"));
			RandomAccessFile raf = new RandomAccessFile(DataBaseManagementSystem.PathOfMainDatabaseDirectory 
					+ name + "/" + name + ".dbtmp", "rw");
			FileChannel newFileDB = raf.getChannel();
			fileDB.close();
			fileDB = newFileDB;
			List<Document> newDocs = new CopyOnWriteArrayList<>();
			docs.stream().filter(x->!x.isDelete()).forEach(x->{
				try {
					addDocumentInPhysicalStorage(x);
					newDocs.add(x);
				} catch (IOException e) {}
			});
			docs = newDocs;
			Files.delete(Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory 
					+ name + "/" + name + ".db"));
			Files.move(Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory + name + "/" + name + 
					".dbtmp"), Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory + name + "/" + 
							name + ".db"));
		}
	}
	
	/**
	 * load the database in memory
	 * @return a List containing all the documents of the database file
	 * @throws IOException if it is not possible to find the database repository or file
	 */
	public List<Document> loadDB() throws IOException{
		raf = new RandomAccessFile("./Database/" + name + "/" + name + ".db", "rw");
		fileDB = raf.getChannel();
		MappedByteBuffer mbb = fileDB.map(FileChannel.MapMode.READ_WRITE, 0, fileDB.size());
		return Parser.parser(mbb);
	}

	private void loadIndexs() throws IOException{
		DirectoryStream<Path> DBDirectory;
		DBDirectory = Files.newDirectoryStream(Paths.get("./Database/" + name));
		DBDirectory.forEach(x->{
			if(x.toFile().getName().startsWith("index_")){
				try(RandomAccessFile raf = new RandomAccessFile("./Database/" + name + "/" + x.toFile().getName(), "r")){
					try(FileChannel fileIndex = raf.getChannel()){
						loadIndex(x.toFile().getName().substring(6),fileIndex.map(FileChannel.MapMode.READ_ONLY, 0, fileIndex.size()));
					}
				} catch (IOException e) {}
			}
		});
		DBDirectory.close();
	}

	private void loadIndex(String fieldIndex, MappedByteBuffer indexMbb){
		TreeMap<Object, List<Document>> index = new TreeMap<>();
		byte b;

		for(indexMbb.position(0); indexMbb.position() < indexMbb.limit();){
			StringBuilder value = new StringBuilder();
			for(b = indexMbb.get(); b != ',' && indexMbb.position() < indexMbb.limit(); b = indexMbb.get()){
				value.append((char)b);
			}
			if(b != ',')
				value.append((char)b);
			int indexOfDocument = Integer.valueOf(value.toString());
			docs.forEach(x->{
				if(((int)x.getValues().get("indexOfDocument").getValue()) == indexOfDocument)
					addADocInAIndex(index, x, fieldIndex);
			});
		}
		indexs.put(fieldIndex, index);
	}

	private void addADocInAIndex(TreeMap<Object, List<Document>> index, Document doc, String fieldIndex){
		GenericValue value = doc.getValues().get(fieldIndex);
		if(!(index.containsKey(value.getValue()))){
			index.put(value.getValue(), new CopyOnWriteArrayList<>());
		}
		index.get(value.getValue()).add(doc);
	}

	private void dropADocInAIndex(TreeMap<Object, List<Document>> index, Document doc, String fieldIndex){
		synchronized (indexs) {
			GenericValue value = doc.getValues().get(fieldIndex);
			index.get(value.getValue()).remove(doc);
		}
	}

	/**
	 * unload the database object in memory
	 * @throws IOException if it is not possible to save the database without deleted document
	 */
	public void unloadDB() throws IOException{
		createNewDatabase();
		name = ""; // Delete the name of the database
		docs.clear(); // Remove all documents of the database 
		indexs.clear(); // Remove all index of the database
		fileDB.close();
		raf.close();
	}

	/**
	 * Get documents corresponding to the request in the database
	 * @param request = The criteria of selecting documents
	 * @return a Stream containing all documents corresponding to the request in the database
	 */
	public Stream<Document> get(String request) {
		return getDocumentsOfCriters(request);
	}

	/**
	 * Delete documents of the database corresponding to the name of document in the parameter request
	 * @param nameOfDoc = the name of the document to delete
	 * 
	 */
	public void drop(String nameOfDoc) {
		synchronized(indexs){
			List<Document> docsToDelete = indexs.get("name_doc").get(nameOfDoc);
			System.out.println(indexs.get("name_doc"));
			System.out.println();
			System.out.println(docsToDelete);
			for(Document doc : docsToDelete){
				doc.setToDelete();
				nbToDelete++;
				doc.getValues().forEach((a,b)->{
					dropADocInAIndex(indexs.get(a), doc, a);
					try {
						updateIndexInPhysicalStorage(a);
					} catch (IOException e) {}
				});
			}
			indexs.notifyAll();
		}
	}

	/**
	 * Insert the document in the database
	 * @param body = a JsonObject in String containing all the fields and associated values of the new document
	 * @throws IOException if it is not possible to create on the physical storage
	 */
	public void create(JsonObject body) throws IOException {
		synchronized(indexs){
			if(!body.containsKey("name_doc"))
				throw new IllegalArgumentException("the body must contains the name_doc field");
			Document doc = Parser.parseJSONToDocument(body);
			doc.getValues().put("indexOfDocument", new IntegerValue(Integer.toString(docs.size()+1)));
			docs.add(doc);
			doc.getValues().forEach((x,y)->{
				if(!indexs.containsKey(x))
					indexs.put(x, new TreeMap<>());
				addADocInAIndex(indexs.get(x),doc,x);
				try {
					updateIndexInPhysicalStorage(x);
				} catch (IOException e) {}
			});
			addDocumentInPhysicalStorage(doc);
		}
	}

	private void addDocumentInPhysicalStorage(Document doc) throws IOException{
		String s = Parser.getStringToDocument(doc);
		fileDB.write(ByteBuffer.wrap(s.getBytes()));
	}

	private void updateIndexInPhysicalStorage(String indexName) throws IOException{
		Files.createFile(Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory + name + "/" 
				+ "tmpindex_" + indexName ));
		try(RandomAccessFile raf = new RandomAccessFile(DataBaseManagementSystem.PathOfMainDatabaseDirectory 
				+ name + "/" + "tmpindex_" + indexName, "rw")){
			try(FileChannel newFileDB = raf.getChannel()){
				newFileDB.write(ByteBuffer.wrap(getStringOfIndex(indexName).getBytes()));
			}
		}
		if(Files.exists(Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory + name + "/" 
				+ "index_" + indexName ))){
			Files.delete(Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory 
					+ name + "/" + "index_" + indexName));
		}
		Files.move( Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory + name + "/" 
				+ "tmpindex_" + indexName), 
				Paths.get(DataBaseManagementSystem.PathOfMainDatabaseDirectory + name + "/" + 
						"index_" + indexName));	
	}

	private String getStringOfIndex(String indexName){
		StringBuilder sb = new StringBuilder();
		indexs.get(indexName).forEach((x,y)->{
			y.forEach(d->sb.append(d.getValues().get("indexOfDocument").getValue()).append(","));
		});
		sb.setLength(sb.length()-1);
		return sb.toString();
	}

	private Stream<Document> getDocumentsOfCriters(String criters){
		CopyOnWriteArrayList<Stream<Document>> res = new CopyOnWriteArrayList<>();
		for(String criter : criters.split("and")){
			res.add(getDocumentsOfCriter(criter));
		}
		return res.stream().flatMap(x->x).distinct();
	}

	private Stream<Document> getDocumentsOfCriter(String criter){
		criter = criter.trim();
		String[] values = criter.split("\"");
		String field = values[1];
		String op = values[2];
		String criter1 = values[3];
		TreeMap<Object, List<Document>> index = indexs.get(field);

		switch(op){
		case "=":
			return (index.get(criter1)==null)?Arrays.stream(new Document[0]):index.get(criter1).stream();
		case ">":
			return index.tailMap(criter1, false).values().stream().flatMap(x->{return x.stream();});	
		case ">=":
			return index.tailMap(criter1, true).values().stream().flatMap(x->{return x.stream();});
		case "<":
			return index.headMap(criter1, false).values().stream().flatMap(x->{return x.stream();});
		case "<=":
			return index.headMap(criter1, true).values().stream().flatMap(x->{return x.stream();});
		case "between":
			return index.subMap(criter1, values[5]).values().stream().flatMap(x->{return x.stream();});
		default:
			throw new IllegalArgumentException("L'opération " + op + " n'est pas connu");
		}

	}
}
