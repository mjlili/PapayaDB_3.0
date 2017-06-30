package fr.umlv.papayaDB.databaseManagementSystem.document;

import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.vertx.core.json.JsonObject;

public class Parser {

	/**
	 * Transform a JsonObject to a Document
	 * @param body = all fields and their values of the document
	 * @return a document
	 * @throws IllegalArgumentException if the char type it'snt recognized
	 */
	public static Document parseJSONToDocument(JsonObject body){
		HashMap<String, GenericValue> values = new HashMap<>();
		body.forEach(x->{
			String key = x.getKey().substring(1, x.getKey().length());
			GenericValue gValue;		
			
			switch(x.getKey().charAt(0)){
				case 's':
					gValue = new StringValue((String)x.getValue());
					break;
				case 'i':
					gValue = new IntegerValue((String)x.getValue());
					break;
				case 'd':
					try {
						gValue = new DateValue((String)x.getValue());
					} catch (ParseException e) {
						throw new IllegalArgumentException("");
					}
					break;
				default:
					throw new IllegalArgumentException("");	
			}
			values.put(key, gValue);
		});
		return new Document(values);
	}
	
	/**
	 * Read the Database file and parse it to obtain the list of documents containing in the database
	* @param mbb a MappedBytesBuffer to parse
	 * @return a List containing all documents in the database file
	 */
	public static List<Document> parser(MappedByteBuffer mbb){
		List<Document> docs = new ArrayList<>();
		boolean done = false;
		for(;done != true;){
			try{
				docs.add(getADocument(mbb));
			} catch(BufferUnderflowException e) {
				done = true;
			}
		}
		return docs;
	}
	
/**
	 * Read the Database file and parse it to obtain the list of documents containing in the database
	* @param mbb a MappedBytesBuffer where we look for a document
	 * @return a List containing all documents in the database file
	 */
	private static Document getADocument(MappedByteBuffer mbb){
		HashMap<String, GenericValue> values = new HashMap<>();
		
		for(byte b = mbb.get(); b != '{'; b = mbb.get()); // find the first {
		while(mbb.get() != '}'){
			mbb.position(mbb.position()-1);
			try {
				values.put(getValue(mbb), getGenericValue(mbb));
			} catch (ParseException e) {}
		}
		return new Document(values);	
	}
	/**
	 * Read the Database file and parse it to obtain the list of documents containing in the database
	* @param mbb a MappedBytesBuffer where we look for a value
	 * @return a List containing all documents in the database file
	 */
	private static String getValue(MappedByteBuffer mbb){
		StringBuilder value = new StringBuilder();
		
		for(byte b = mbb.get(); b != ':' && b != ';'; b = mbb.get()){
			value.append((char)b);
		}
		return value.toString();
	}
	
/**
	 * Read the Database file and parse it to obtain the list of documents containing in the database
	* @param mbb a MappedBytesBuffer where we look for a generic value
	 * @return a List containing all documents in the database file
	 */
	private static GenericValue getGenericValue(MappedByteBuffer mbb) throws ParseException{
		switch(mbb.get()){
			case 's':
				return new StringValue(getValue(mbb));
			case 'i':
				return new IntegerValue(getValue(mbb));
			case 'd':
				return new DateValue(getValue(mbb));
			default:
				return null;
		}
	}
	
	/**
	 * View the document likes a String
	 * @param doc the document to view like a string
	 * @return a string containing all the fields and their values of the document
	 */
	public static String getStringToDocument(Document doc){
		StringBuilder sb = new StringBuilder("{");
		doc.getValues().forEach((x,y)->{
			sb.append(x).append(":").append(y.getCharType()).append(y.getValue()).append(";");
		});
		sb.append("}");
		return sb.toString();
	}
}
