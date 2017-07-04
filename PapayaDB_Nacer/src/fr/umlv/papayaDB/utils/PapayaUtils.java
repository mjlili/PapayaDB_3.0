package fr.umlv.papayaDB.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PapayaUtils {

	public static List<ObjectNode> extractJsonObjectsFromFile(String file) throws JsonParseException, IOException {
		JsonParser jsonParser = new JsonFactory().createParser(new File(file));
		List<ObjectNode> documents = extractJsonObjectsByJsonParser(jsonParser);
		return documents;
	}

	public static List<ObjectNode> extractJsonObjectsFromString(String string) throws JsonParseException, IOException {
		JsonParser jsonParser = new JsonFactory().createParser(string);
		List<ObjectNode> documents = extractJsonObjectsByJsonParser(jsonParser);
		return documents;
	}

	private static List<ObjectNode> extractJsonObjectsByJsonParser(JsonParser jsonParser)
			throws IOException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		List<ObjectNode> documents = new LinkedList<ObjectNode>();
		MappingIterator<ObjectNode> objectNodesIterator = mapper.readValues(jsonParser, ObjectNode.class);
		while (objectNodesIterator.hasNext()) {
			ObjectNode objectNode = objectNodesIterator.next();
			objectNode.putNull("_id");
			documents.add(objectNode);
		}
		return documents;
	}

	/**
	 * returns true if the data base already exists
	 *
	 * @param databasename
	 *            a string containing the data base name
	 * @return boolean exists or not
	 */
	public static boolean databaseExists(String databaseName) {
		File databaseDirectory = new File("./Database");
		File[] files = databaseDirectory.listFiles();
		if (Arrays.stream(files).filter(database -> database.getName().equals(databaseName)).count() == 0) {
			return false;
		}
		return true;
	}
}
