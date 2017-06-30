package fr.umlv.papayaDB.databaseManagementSystem.document;

public class StringValue implements GenericValue{
	private String value;
	
	/**
	 * Constructor
	 * @param value = the value in String
	 */
	public StringValue(String value){
		this.value = value;
	}

	/**
	 * Get the value containing in this object
	 * @return the value containing in this object
	 */
	@Override
	public Object getValue() {
		return value;
	}

	/**
	 * Get the char corresponding to this type
	 * @return the char of this type
	 */
	@Override
	public char getCharType() {
		return 's';
	}
}
