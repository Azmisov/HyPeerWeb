package hypeerweb;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Hold name-value pair for attributes
 * @author John
 */
public class Attributes implements Serializable{
	private final HashMap<String, Object> attributes = new HashMap<>();

	/**
	 * Set a data attribute
	 * @param name the name of the attribute (key)
	 * @param value the data to hold under this name
	 */
	public void setAttribute(String name, Object value){
		attributes.put(name, value);
	}
	/**
	 * Retrieve data stored under a particular name
	 * @param name the name the data was stored under
	 * @return the data object, or null, if it doesn't exist
	 */
	public Object getAttribute(String name){
		return attributes.get(name);
	}
}
