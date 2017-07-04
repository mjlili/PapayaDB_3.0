package fr.umlv.papayaDB.databaseManagement;

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

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.umlv.papayaDB.databaseManagement.document.Document;
import fr.umlv.papayaDB.databaseManagement.document.GenericValue;
import fr.umlv.papayaDB.databaseManagement.document.Parser;

public class Database {
	private List<Document> documents;
	private String databaseName;
	private final HashMap<String, TreeMap<Object, List<Document>>> indexes = new HashMap<>();
	private FileChannel databaseFileChannel;
	private RandomAccessFile randomAccessFile;
	private volatile int nbToDelete = 0;

	/**
	 * Constructor
	 * 
	 * @param databaseName
	 *            = the name of the database
	 * @throws IOException
	 *             if it'snt possible to load the database
	 */
	public Database(String databaseName) throws IOException {
		this.databaseName = databaseName;
		documents = loadDatabase();
		loadIndexes();
		launchThreadDelete();
	}

	public String getDatabaseName() {
		return this.databaseName;
	}

	public List<Document> getDocuments() {
		return this.documents;
	}

	private void launchThreadDelete() {
		Thread t = new Thread(() -> {
			for (;;) {
				synchronized (indexes) {
					while (nbToDelete / ((documents.size() == 0) ? 1 : documents.size()) < 0.8) {
						try {
							indexes.wait();
						} catch (InterruptedException e) {
						}
					}
					try {
						createNewDatabase();
						nbToDelete = 0;
					} catch (IOException e) {
					}
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	private void createNewDatabase() throws IOException {
		synchronized (indexes) {
			Files.createFile(Paths
					.get(DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + databaseName + ".dbtmp"));
			RandomAccessFile raf = new RandomAccessFile(
					DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + databaseName + ".dbtmp", "rw");
			FileChannel newFileDB = raf.getChannel();
			databaseFileChannel.close();
			databaseFileChannel = newFileDB;
			List<Document> newDocs = new CopyOnWriteArrayList<>();
			documents.stream().filter(x -> !x.isDelete()).forEach(x -> {
				try {
					addDocumentOnDisk(x);
					newDocs.add(x);
				} catch (IOException e) {
				}
			});
			documents = newDocs;
			Files.delete(
					Paths.get(DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + databaseName + ".db"));
			Files.move(
					Paths.get(
							DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + databaseName + ".dbtmp"),
					Paths.get(DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + databaseName + ".db"));
		}
	}

	/**
	 * load the database in memory
	 * 
	 * @return a List containing all the documents of the database file
	 * @throws IOException
	 *             if it is not possible to find the database repository or file
	 */
	public List<Document> loadDatabase() throws IOException {
		randomAccessFile = new RandomAccessFile("./Database/" + databaseName + "/" + databaseName + ".db", "rw");
		databaseFileChannel = randomAccessFile.getChannel();
		MappedByteBuffer mbb = databaseFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, databaseFileChannel.size());
		return Parser.parse(mbb);
	}

	private void loadIndexes() throws IOException {
		DirectoryStream<Path> databaseDirectory;
		databaseDirectory = Files.newDirectoryStream(Paths.get("./Database/" + databaseName));
		databaseDirectory.forEach(x -> {
			if (x.toFile().getName().startsWith("index_")) {
				try (RandomAccessFile raf = new RandomAccessFile(
						"./Database/" + databaseName + "/" + x.toFile().getName(), "r")) {
					try (FileChannel fileIndex = raf.getChannel()) {
						loadIndex(x.toFile().getName().substring(6),
								fileIndex.map(FileChannel.MapMode.READ_ONLY, 0, fileIndex.size()));
					}
				} catch (IOException e) {
				}
			}
		});
		databaseDirectory.close();
	}

	// Trouver l'erreur de NullPointerException
	private void loadIndex(String fieldIndex, MappedByteBuffer indexMbb) {
		TreeMap<Object, List<Document>> index = new TreeMap<>();
		byte b;
		for (indexMbb.position(0); indexMbb.position() < indexMbb.limit();) {
			StringBuilder value = new StringBuilder();
			for (b = indexMbb.get(); b != ',' && indexMbb.position() < indexMbb.limit(); b = indexMbb.get()) {
				value.append((char) b);
			}
			if (b != ',')
				value.append((char) b);
			int documentIndex = Integer.valueOf(value.toString());
			for (Document document : documents) {
				if (((int) document.getValues().get("documentIndex").getValue()) == documentIndex)
					addDocumentAtIndex(index, document, fieldIndex);
			}
			// documents.forEach(x -> {
			// if (((int) x.getValues().get("documentIndex").getValue()) ==
			// documentIndex)
			// addDocumentAtIndex(index, x, fieldIndex);
			// });
		}
		indexes.put(fieldIndex, index);
	}

	private void addDocumentAtIndex(TreeMap<Object, List<Document>> index, Document doc, String fieldIndex) {
		GenericValue value = doc.getValues().get(fieldIndex);
		if (!(index.containsKey(value.getValue()))) {
			index.put(value.getValue(), new CopyOnWriteArrayList<>());
		}
		index.get(value.getValue()).add(doc);
	}

	private void dropADocInAIndex(TreeMap<Object, List<Document>> index, Document doc, String fieldIndex) {
		synchronized (indexes) {
			GenericValue value = doc.getValues().get(fieldIndex);
			index.get(value.getValue()).remove(doc);
		}
	}

	/**
	 * unload the database object in memory
	 * 
	 * @throws IOException
	 *             if it is not possible to save the database without deleted
	 *             document
	 */
	public void unloadDatabase() throws IOException {
		createNewDatabase();
		databaseName = ""; // Delete the name of the database
		documents.clear(); // Remove all documents of the database
		indexes.clear(); // Remove all index of the database
		databaseFileChannel.close();
		randomAccessFile.close();
	}

	/**
	 * Get documents corresponding to the request in the database
	 * 
	 * @param request
	 *            = The criteria of selecting documents
	 * @return a Stream containing all documents corresponding to the request in
	 *         the database
	 */
	public Stream<Document> get(String request) {
		return getDocumentsOfCriters(request);
	}

	/**
	 * Delete documents of the database corresponding to the name of document in
	 * the parameter request
	 * 
	 * @param documentName
	 *            = the name of the document to delete
	 * 
	 */
	public void drop(String documentName) {
		synchronized (indexes) {
			List<Document> documentsToDelete = indexes.get("name_doc").get(documentName);
			System.out.println(indexes.get("name_doc") + "\n");
			System.out.println(documentsToDelete);
			for (Document doc : documentsToDelete) {
				doc.setToDelete();
				nbToDelete++;
				doc.getValues().forEach((a, b) -> {
					dropADocInAIndex(indexes.get(a), doc, a);
					try {
						updateIndexOnDisk(a);
					} catch (IOException e) {
					}
				});
			}
			indexes.notifyAll();
		}
	}

	/**
	 * Insert the document in the database
	 * 
	 * @param documentContent
	 *            = a JsonObject in String containing all the fields and
	 *            associated values of the new document
	 * @throws IOException
	 *             if it is not possible to create on the physical storage
	 */
	public void insertDocument(ObjectNode documentContent) throws IOException {
		synchronized (indexes) {
			// if (!documentContent.containsKey("name_doc"))
			// throw new IllegalArgumentException("the body must contains the
			// name_doc field");

			documentContent.put("documentIndex", Integer.toString(documents.size() + 1));
			Document document = Parser.parseJSONToDocument(documentContent);
			documents.add(document);
			document.getValues().forEach((x, y) -> {
				if (!indexes.containsKey(x))
					indexes.put(x, new TreeMap<>());
				addDocumentAtIndex(indexes.get(x), document, x);
				try {
					updateIndexOnDisk(x);
				} catch (IOException e) {
				}
			});
			addDocumentOnDisk(document);

			// Document newDocument = new Document(documentContent);
			// documents.add(newDocument);
			// addDocumentOnDisk(newDocument);
		}
	}

	private void addDocumentOnDisk(Document document) throws IOException {
		String s = Parser.getStringToDocument(document);
		databaseFileChannel.write(ByteBuffer.wrap(s.getBytes()));
		// databaseFileChannel.write(ByteBuffer.wrap(document.getJsonContent().toString().getBytes()));
	}

	private void updateIndexOnDisk(String indexName) throws IOException {
		Files.createFile(
				Paths.get(DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + "tmpindex_" + indexName));
		try (RandomAccessFile raf = new RandomAccessFile(
				DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + "tmpindex_" + indexName, "rw")) {
			try (FileChannel newFileDB = raf.getChannel()) {
				newFileDB.write(ByteBuffer.wrap(getStringOfIndex(indexName).getBytes()));
			}
		}
		if (Files.exists(
				Paths.get(DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + "index_" + indexName))) {
			Files.delete(
					Paths.get(DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + "index_" + indexName));
		}
		Files.move(Paths.get(DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + "tmpindex_" + indexName),
				Paths.get(DatabaseManager.PATH_OF_DATABASE_DIRECTORY + databaseName + "/" + "index_" + indexName));
	}

	private String getStringOfIndex(String indexName) {
		StringBuilder sb = new StringBuilder();
		indexes.get(indexName).forEach((x, y) -> {
			y.forEach(d -> sb.append(d.getValues().get("documentIndex").getValue()).append(","));
		});
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	private Stream<Document> getDocumentsOfCriters(String criters) {
		CopyOnWriteArrayList<Stream<Document>> res = new CopyOnWriteArrayList<>();
		for (String criter : criters.split("and")) {
			res.add(getDocumentsOfCriter(criter));
		}
		return res.stream().flatMap(x -> x).distinct();
	}

	private Stream<Document> getDocumentsOfCriter(String criter) {
		criter = criter.trim();
		String[] values = criter.split("\"");
		String field = values[1];
		String op = values[2];
		String criter1 = values[3];
		TreeMap<Object, List<Document>> index = indexes.get(field);

		switch (op) {
		case "=":
			return (index.get(criter1) == null) ? Arrays.stream(new Document[0]) : index.get(criter1).stream();
		case ">":
			return index.tailMap(criter1, false).values().stream().flatMap(x -> {
				return x.stream();
			});
		case ">=":
			return index.tailMap(criter1, true).values().stream().flatMap(x -> {
				return x.stream();
			});
		case "<":
			return index.headMap(criter1, false).values().stream().flatMap(x -> {
				return x.stream();
			});
		case "<=":
			return index.headMap(criter1, true).values().stream().flatMap(x -> {
				return x.stream();
			});
		case "between":
			return index.subMap(criter1, values[5]).values().stream().flatMap(x -> {
				return x.stream();
			});
		default:
			throw new IllegalArgumentException("L'opération " + op + " n'est pas connu");
		}

	}
}
