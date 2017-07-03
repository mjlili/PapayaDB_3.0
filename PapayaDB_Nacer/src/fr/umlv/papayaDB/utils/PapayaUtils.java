package fr.umlv.papayaDB.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.json.JsonObject;

public class PapayaUtils {

	public static List<JsonObject> extractJsonObjectsFromFile(String file) throws JsonParseException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		List<JsonObject> documents = new LinkedList<JsonObject>();
		JsonParser jsonParser = new JsonFactory().createParser(new File(file));
		MappingIterator<JsonObject> jsonObjectsIterator = mapper.readValues(jsonParser, JsonObject.class);
		while (jsonObjectsIterator.hasNext()) {
			JsonObject jsonObject = jsonObjectsIterator.next();
			jsonObject.putNull("_id");
			documents.add(jsonObject);
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
