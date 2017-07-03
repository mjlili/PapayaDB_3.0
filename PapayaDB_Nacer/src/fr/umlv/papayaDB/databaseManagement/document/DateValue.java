package fr.umlv.papayaDB.databaseManagement.document;


import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class DateValue implements GenericValue{
	private Date value;
	
	/**
	 * Constructor
	 * @param value = the date in String
	 * @throws ParseException it'snt possible to convert the value to Date
	 */
	public DateValue(String value) throws ParseException{
		DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
		this.value = df.parse(value);
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
		return 'd';
	}
}
