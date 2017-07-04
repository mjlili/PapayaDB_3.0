package fr.umlv.papayaDB.databaseManagement.document;

import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class Document {
	HashMap<String, GenericValue> values;
	boolean toDelete = false;
	private ObjectNode jsonContent;

	/**
	 * Constructor
	 * 
	 * @param values
	 *            = a HashMap containing all the fields and their values of the
	 *            document
	 */
	public Document(HashMap<String, GenericValue> values) {
		this.values = values;
	}

	public Document(ObjectNode jsonContent) {
		this.jsonContent = Objects.requireNonNull(jsonContent);
	}

	public ObjectNode getJsonContent() {
		return jsonContent;
	}

	/**
	 * Get all the fields and their values of the document
	 * 
	 * @return a HashMap containing all the fields and their values of the
	 *         document
	 */
	public HashMap<String, GenericValue> getValues() {
		return values;
	}

	/**
	 * Look if this document has marked deleted
	 * 
	 * @return the delete status of this document
	 */
	public boolean isDelete() {
		return toDelete;
	}

	/**
	 * Marks this document to delete
	 */
	public void setToDelete() {
		toDelete = true;
	}

	/**
	 * View this document likes a String
	 * 
	 * @return a string containing all the fields and their values of the
	 *         document
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		values.forEach((x, y) -> {
			sb.append(x).append(":").append(y.getValue()).append(";");
		});
		sb.append("}");
		return sb.toString();
	}
}
