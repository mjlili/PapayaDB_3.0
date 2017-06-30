package fr.umlv.papayaDB.utils;

import java.util.Base64;

/**
 * @author JLILI Mohamed Kacem & REZGUI Ichrak
 *
 */
public class Decoder {
	/** Constructeur de l'API Cliente
	 * @param string chaine de caracteres a decoder
	 * @return string la chaine de caracteres decode
	 */
	public static String decode(String string) {
		return new String(Base64.getDecoder().decode(string.getBytes()));
	} 
	/** Constructeur de l'API Cliente
	 * @param string chaine de caracteres a coder
         * @return string la chaine de caracteres decode
	 */
	public static String encode(String string){
		 return Base64.getEncoder().encodeToString(string.getBytes() );
	}
	
}
