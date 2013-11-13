package communicator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

/**
 * A persistent list of all identifiable objects in an application.  While this implementation retrieves the
 * objects from a file on startup, and stores them in a file on shutdown, the persistence could be implemented
 * by storing the objects in the applications database.  This is a singleton.
 * 
 * <pre>
 * <b>Class Domain:</b>
 *     FILE_LOCATION : String -- the location of the file where this ObjectDB is stored.
 *     
 *     <b>Invariant:</b>
 *         FILE_LOCATION &ne; null
 *         
 * <b>Instance Domain:</b>
 *     hashTable : HashTable<LocalObjectId, Object>
 * </pre>
 * 
 * @author Scott Woodfield
 */
public class ObjectDB { //A Singleton
	
//Singleton Implementation
	/**
	 * The single instance of the class.
	 */
	private static ObjectDB singleton = null;
	
	/**
	 * The singleton getter
	 * 
	 * @pre <i>None</i>
	 * @post singleton = null &rArr; result = new ObjectDB() ELSE result = singleton
	 */
	public static ObjectDB getSingleton(){
		if(singleton == null){
			singleton = new ObjectDB();
		}
		return singleton;
	}
	
	/**
	 * Sets the file location from where the ObjectDB will be retrieved on startup and stored at shutdown.
	 * @param fileLocation the name of the file location
	 * 
	 * @pre fileLocation &ne; null
	 * @post FILE_LOCATION = fileLocation
	 */
	public static void setFileLocation(String fileLocation){
		assert fileLocation == null;
		
		FILE_LOCATION = fileLocation;
	}
//Domain Implementation
	/**
	 * The in memory implementation of the ObjectDB. Implemented for speed.
	 */
    private Hashtable<LocalObjectId, Object> hashTable = null;

//Constructors
    
    /**
     * The default constructor.
     * 
     * @pre <i>None</i>
     * @post hashTable = new HashTable<LocalObjectId, Object>()
     */
    private ObjectDB(){
    	
    	hashTable =  new Hashtable<LocalObjectId, Object>();
    }
    
//Queries
    /**
     * Retrieves an object from the ObjectDB with the indicated key.
     * 
     * @param key the key to the object in the ObjectDB
     * @pre <i>None</i>
     * @post result = hashTable.get(key)
     */
    public Object getValue(LocalObjectId key) {
    	Object result = hashTable.get(key);
    	return result;
    }
    
    /**
     * Returns the size of the ObjectDB.
     * 
     * @pre <i>None</i>
     * @post result = |hashTable|
     */
    public int getSize(){
    	return hashTable.size();
    }
    
    /**
     * Returns a random member of the ObjectDB.
     * 
     * @pre <i>None</i>
     * @post getSize() = 0 &rArr; result = null
     *       ELSE result = random member of the hashTable
     */
    public Object getRandomMember() {
    	//Assumes the DB never has more than Integer.MAX_VALUE members
    	int randomMemberIndex = random.nextInt(hashTable.size());
    	return getRandomMember(hashTable.size());
    }
    
    /**
     * Returns the ith memeber of the ObjectDB. 
     * 
     * @param i indicates the value of the ObjectDB to select.
     * @pre 0 &le; i &lt; |hashTable|
     * @post given a random ordering defined over the members of the ObjectDB, return the ith value
     */
    public Object getRandomMember(int i){
    	//Assumes the DB never has more than Integer.MAX_VALUE members
    	int randomMemberIndex = random.nextInt(i);
    	int count = 0;
    	Enumeration<Object> enumeration = hashTable.elements();
    	Object result = null;
    	while(enumeration.hasMoreElements()){
    		result = enumeration.nextElement();
    	if(count == randomMemberIndex)break;
    		count++;
    	}
    	return result;
    }
    
    /**
     * Returns an enumeration of the ObjectDB.
     * 
     * @pre <i>None</i>
     * @post result = hashTable.elements()
     */
    public Enumeration<Object> enumeration(){
    	return hashTable.elements();
    }
    
    /**
     * Returns the string representation of the ith Node.
     * 
     * @param i the index of the node under consideration.
     * @pre 0 &le; i &lt; |hashTable|
     * @post result = "Node " + i + "\n" + getNode(i).toString()
     */
    /*
    public void showNode(int i){
    	Node result = getNode(i);

    	System.out.println("Node " + i + "\n" + result);
    }
    */
    
    /**
     * Returns the node in the ObjectDB with the webId whose value = i.
     * 
     * @param i the value of the webId we are searching for.
     * 
     * @pre <i>None</i>
     * @post result = null &rArr; &not; &exist; key(hashTable.containsKey(key) AND result = hashTable.get(key) AND result.getWebId().getValue() = i) AND
     *       result &ne; null &rArr; result.getWebId().getValue() = i AND &exist; key(hashTable.containsKey(key) AND result = hashTable.get(key))
     */
    /*
    public Node getNode(int i){
    	WebId webId = new WebId(i);
    	Node result = null;
    	
    	Enumeration<LocalObjectId> enumerator = hashTable.keys();
    	while(result == null && enumerator.hasMoreElements()){
    		Node possibleNode = (Node)hashTable.get(enumerator.nextElement());
    		if(possibleNode.getWebId().equals(webId)){
    			result = possibleNode;
    		}
    	}
    	return result;
    }
    */
    
    /**
     * Outputs a list of objects in the ObjectDB. Used mostly for debugging.
     * 
     * @pre <i>None</i>
     * @post a list of all the nodes in the ObjectDB is printed to System.out.  
     */
    public void dump(){
    	System.out.println("==========================================================================================================");
    	//System.out.println("ObjectDB Dump with " + hashTable.size() + " members");
    	//System.out.println(hashTable.size() + " Node HyPeerWeb\n");
    	Enumeration<LocalObjectId> enumerator = hashTable.keys();
    	while(enumerator.hasMoreElements()){
            LocalObjectId key = enumerator.nextElement();
    		System.out.println("Object with key " + key + " = " + hashTable.get(key));
    	}
    	System.out.println("==========================================================================================================");
    }
    
//Commands
    /**
     * The setter for the ObjectDB.
     * 
     * @pre key &ne; null
     * &post get(key) = value
     */
    public void store(LocalObjectId key, Object value) {
    	hashTable.put(key, value);
    }
    
    /**
     * Remove the object from the database associated with the key.
     * 
     * @param key the key to the object to be removed.
     * 
     * @pre key &ne; null
     * @post &not; hashTable.contains(key) 
     */
    public void remove(LocalObjectId key){
    	hashTable.remove(key);
    }
    
    /**
     * Empties the ObjectDB and sets the next LocalObjectID to the initial LocalObjectId.
     * 
     * @pre <i>None</i>
     * @post |hashTable| = 0 AND LocalObjectId.nextId = LocalObjectId.INITIAL_ID
     */
    public void clear(){
    	hashTable.clear();
    	LocalObjectId.setNextId(LocalObjectId.INITIAL_ID);
    }
    
    /**
     * Saves the hashTable in the file specified by the <i>destination</i>.
     *      * 
     * @pre destination &ne; null AND location in file system where file is to be created exists AND
     *      the file location is writable AND every object in the ObjectDB is serializable.
     * @post the hashTable has been saved to the file named <i>destination</i> along with the size of the hashTable.
     */
    public void save(String destination){
        String fileLocation = DEFAULT_FILE_LOCATION;
        if(FILE_LOCATION != null){
            fileLocation = FILE_LOCATION;
        }
        if(destination != null){
             fileLocation = destination;
        }
    	try{
    		ObjectOutputStream oos =
    			new ObjectOutputStream(
    				new BufferedOutputStream(
    					new FileOutputStream(fileLocation)));
    		oos.write(LocalObjectId.getNextId());
    		oos.writeObject(hashTable);
    		oos.flush();
    		oos.close();
    	} catch(Exception e){
    		System.err.println("In communicator.ObjectDB::save(String) -- ERROR could not save ObjectDB");
    	}		
    }
    
    /**
     * Saves the hashTable in the file specified by the <i>FILE_LOCATION</i>.
     * 
     * @pre FILE_LOCATION &ne; null AND location in file system where file is to be created exists AND
     *      the file location is writable AND every object in the ObjectDB is serializable.
     * @post the hashTable has been saved to the file named <i>FILE_LOCATION</i> along with the size of the hashTable.
     */
    public void save(){
    	save(FILE_LOCATION);
    }
    
    /**
     * Retrieves the hashTable from the file specified by <i>source</i>.
     * 
     * @param source the name of the file from which the hashTable is to be retrieved.
     * 
     * @pre <i>None</i>
     * @post If there is any error &rArr; LocalObjectId.nextId = LocalObjectId.INITIAL_ID and |hashTable| = 0
     *       Else the hashTable in the specified file is retrieved and stored in hashTable AND LocalObjectId.nextId = |hashTable|
     */
    public void restore(String source){
        String fileLocation = DEFAULT_FILE_LOCATION;
        if(FILE_LOCATION != null){
            fileLocation = FILE_LOCATION;
        }
        if(source != null){
             fileLocation = source;
        }
    	try{
    		ObjectInputStream ois =
    			new ObjectInputStream(
    				new BufferedInputStream(
    					new FileInputStream(fileLocation)));
    	    int nextId = ois.read();
    		LocalObjectId.setNextId(nextId);
    		hashTable = (Hashtable<LocalObjectId, Object>)ois.readObject();
    		ois.close();
    	} catch(Exception e){
    		LocalObjectId.setNextId(LocalObjectId.INITIAL_ID);
    		hashTable = new Hashtable<LocalObjectId, Object>();
    	}
    }
    
    /**
     * Retrieves the hashTable from the file specified by <i>FILE_LOCATION</i>.
     * 
     * @pre <i>None</i>
     * @post restore(FILE_LOCATION).post_condition
     */
    public void restore(){
    	restore(FILE_LOCATION);
    }
 
//Auxillary Section
    /**
     * The name of the default location where the ObjectDB is to be saved.
     */
    private static String FILE_LOCATION = null;
    
    /**
     * The random generator used to select a random member of the ObjectDB.
     */
    private Random random = new Random();

    /**
     * The default file location.
     */
    private static final String DEFAULT_FILE_LOCATION = "ObjectDB.db";
}