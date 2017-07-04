package fr.umlv.papayaDB.databaseManagement.document;

import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;

public class Parser {

	/**
	 * Transform a JsonObject to a Document
	 * 
	 * @param body
	 *            = all fields and their values of the document
	 * @return a document
	 * @throws IllegalArgumentException
	 *             if the char type it'snt recognized
	 */
	public static Document parseJSONToDocument(JsonNode body) {
		HashMap<String, GenericValue> values = new HashMap<>();
		body.fieldNames().forEachRemaining(key -> {
			GenericValue genericValue;
			String value = body.get(key).asText();
			try {
				Integer.valueOf(value);
				genericValue = new IntegerValue(value);
			} catch (NumberFormatException e) {
				try {
					DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE).parse(value);
					genericValue = new DateValue(value);
				} catch (ParseException e2) {
					genericValue = new StringValue(value);
				}
			}

			// switch (key.charAt(0)) {
			// case 's':
			// genericValue = new StringValue((String) body.get(key).asText());
			// break;
			// case 'i':
			// genericValue = new IntegerValue((String) body.get(key).asText());
			// break;
			// case 'd':
			// try {
			// genericValue = new DateValue((String) body.get(key).asText());
			// } catch (ParseException e) {
			// throw new IllegalArgumentException("");
			// }
			// break;
			// default:
			// throw new IllegalArgumentException("");
			// }
			values.put(key, genericValue);
		});
		System.out.println(values);
		return new Document(values);
	}

	/**
	 * Read the Database file and parse it to obtain the list of documents
	 * containing in the database
	 * 
	 * @param mbb
	 *            a MappedBytesBuffer to parse
	 * @return a List containing all documents in the database file
	 */
	public static List<Document> parse(MappedByteBuffer mbb) {
		List<Document> documents = new ArrayList<>();
		boolean done = false;
		for (; done != true;) {
			try {
				documents.add(getDocument(mbb));
			} catch (BufferUnderflowException e) {
				done = true;
			}
		}
		return documents;
	}

	/**
	 * Read the Database file and parse it to obtain the list of documents
	 * containing in the database
	 * 
	 * @param mbb
	 *            a MappedBytesBuffer where we look for a document
	 * @return a List containing all documents in the database file
	 */
	private static Document getDocument(MappedByteBuffer mbb) {
		HashMap<String, GenericValue> values = new HashMap<>();

		for (byte b = mbb.get(); b != '{'; b = mbb.get())
			; // find the first {
		while (mbb.get() != '}') {
			mbb.position(mbb.position() - 1);
			try {
				values.put(getValue(mbb), getGenericValue(mbb));
			} catch (ParseException e) {
			}
		}
		return new Document(values);
	}

	/**
	 * Read the Database file and parse it to obtain the list of documents
	 * containing in the database
	 * 
	 * @param mbb
	 *            a MappedBytesBuffer where we look for a value
	 * @return a List containing all documents in the database file
	 */
	private static String getValue(MappedByteBuffer mbb) {
		StringBuilder value = new StringBuilder();

		for (byte b = mbb.get(); b != ':' && b != ';'; b = mbb.get()) {
			value.append((char) b);
		}
		return value.toString();
	}

	/**
	 * Read the Database file and parse it to obtain the list of documents
	 * containing in the database
	 * 
	 * @param mbb
	 *            a MappedBytesBuffer where we look for a generic value
	 * @return a List containing all documents in the database file
	 */
	private static GenericValue getGenericValue(MappedByteBuffer mbb) throws ParseException {
		try {
			Integer.valueOf(getValue(mbb));
			return new IntegerValue(getValue(mbb));
		} catch (NumberFormatException e) {
			try {
				DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE).parse(getValue(mbb));
				return new DateValue(getValue(mbb));
			} catch (ParseException e2) {
				return new StringValue(getValue(mbb));
			}
		}
		// switch (mbb.get()) {
		// case 's':
		// return new StringValue(getValue(mbb));
		// case 'i':
		// return new IntegerValue(getValue(mbb));
		// case 'd':
		// return new DateValue(getValue(mbb));
		// default:
		// return null;
		// }
	}

	/**
	 * View the document likes a String
	 * 
	 * @param doc
	 *            the document to view like a string
	 * @return a string containing all the fields and their values of the
	 *         document
	 */
	public static String getStringToDocument(Document doc) {
		StringBuilder sb = new StringBuilder("{");
		doc.getValues().forEach((x, y) -> {
			sb.append(x).append(":").append(y.getValue()).append(",");
		});
		sb.append("}");
		return sb.toString();
	}
}
