package fr.umlv.papayaDB.databaseManagement.document;

public interface GenericValue {
	/**
	 * Get the value containing in this object
	 * @return the value containing in this object
	 */
	Object getValue();
	
	/**
	 * Get the char corresponding to this type
	 * @return the char of this type
	 */
	char getCharType();
}
