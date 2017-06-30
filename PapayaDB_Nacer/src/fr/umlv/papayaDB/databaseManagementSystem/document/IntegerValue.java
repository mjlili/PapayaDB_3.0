package fr.umlv.papayaDB.databaseManagementSystem.document;

public class IntegerValue implements GenericValue{
	private int value;
	
	/**
	 * Constructor
	 * @param value = the integer in String
	 */
	public IntegerValue(String value){
		this.value = Integer.valueOf(value);
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
		return 'i';
	}
}
