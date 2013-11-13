package communicator;

import java.io.Serializable;

/**
 * The id of an object in an application on a machine.  The id must be unique for all identified objects within
 * an application.  Two objects in different applications may have the same <b>LocalObjectId</b>.
 * 
 * <pre>
 * <b>Instance Domain:</b>
 *     id : int     -- for commercial use we may eventually have to make this a long
 *     
 *     <i>Invariant:</i>
 *     -- LocalObjectIds are unique within the current application.
 *     &forall; x,y (x &isin; LocalObjectId AND y &isin; LocalObjectId AND x &ne; y &rArr; x.id &ne; y.id) 
 *     
 * <b> Class Domain:</b>
 *     nextId : int
 *     -- this class keeps track of the values of all LocalObjectIds generated so far.
 *     
 *     <i>Invariant:</b>
 *     &not; &exist; localObjectId (localObjectId &isin; LocalObjectId AND localObjectId.id = nextId)
 *     
 * @author Scott Woodfield
 */
public class LocalObjectId 
    implements Serializable
{
//Class Methods
	/**
	 * Gets the nextId.  Used primarily for saving the value of the nextId in a serialized database when closing down
	 * the application. If using another database, especially a relational database where values
	 * are stored as they change, this method may not be needed and may be eliminated.  See the ObjectDB class.
	 * 
	 * @pre <i>None</i>
	 * @post result = nextId
	 */
	public static int getNextId(){
		return nextId;
	}

	/**
	 * Sets the class variable <i>nextId</i>.  Used when starting this application and restoring the current
	 * <i>nextId</i> from a database.  
	 * 
	 * @param nextId the value of the nextId to be restored.
	 * 
	 * @pre <i>None</i>
	 * @post LocalObjectId.nextId = nextId
	 */
	public static void setNextId(int nextId){
		LocalObjectId.nextId = nextId;
	}
	
//Domain Implementation
	/**
	 * The id of an instance of LocalObjectId.  Used to identify an object in an application on a machine.
	 */
	protected int id;

//Constructors
	/**
	 * Default Constructor.  Creates a LocalObjectId with a unique id.
	 * 
	 * @pre nextId < Integer.MAX_VALUE
	 * @post id = nextId' AND nextId = nextId' + 1
	 */
	public LocalObjectId() {
		id = nextId;
		nextId++;
	}

	/**
	 * Creates a LocalObjectId from a given integer.  Used primarily when testing.
	 * 
	 * @param id the id of the this LocalObjectId
	 * 
	 * @pre <i>None</i>
	 * @post this.id = id
	 */
	public LocalObjectId(int id){
		this.id = id;
	}
	
	/**
	 * Copy constructor.
	 * 
	 * @param the localObjectId we are going to make a copy of.
	 * 
	 * @pre localObjectId &ne; null
	 * @post this.id = localObjectId.id
	 */
	public LocalObjectId(LocalObjectId localObjectId){
		this.id = localObjectId.id;
	}

//Queries
	/**
	 * The equality operator for LocalObjectIds.
	 * 
	 * @pre <i>None</i>
	 * result = localObjectId &ne; null AND localObjectId &isin; LocalObjectId AND id = logalObjectId.id
	 */
	public boolean equals(Object localObjectId){
		boolean result = localObjectId != null && localObjectId instanceof LocalObjectId && id == ((LocalObjectId)localObjectId).id;
		return result;
	}
	
	/**
	 * The getter for the id.
	 * 
	 * @pre <i>None</i>
	 * @post result = id
	 */
	public int getId(){
		return id;
	}
	
	/**
	 * The hashCode for a localObjectId.  Used for getting a globalObjectId from a hash set or hash table.
	 * 
	 * @pre <i>None</i>
	 * @post result = id
	 */
	public int hashCode() {
		return id;
	}
	
	/**
	 * Returns the string representation of a LocalObjectId.
	 * 
	 * @pre <i>None</i>
	 * @post result = Integer.toString(id)
	 */
	public String toString(){return Integer.toString(id);}

//Auxiliary Section
	/**
	 * Needed when serializing a LocalObjectId so it can be sent across the net.
	 */
	private static final long serialVersionUID = 2910164465720674112L;
	
	/**
	 * The initial or first value of all localObjectIds.
	 */
	public static final int INITIAL_ID = Integer.MIN_VALUE;
	
	/**
	 * The default initialization of the calls variable <i>nextId</i>. Usually overridden by <i>setNextId(int)</i>
	 */
	private static int nextId = INITIAL_ID; 
}