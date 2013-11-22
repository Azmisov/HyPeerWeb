package communicator;

/**
 * The defines a port number used for interprocess communication.  For any service such as the HyPeerWeb there may be one
 * or more application instances running on one or more machines.  A machine may have one or more instances running on the
 * machine.  Each application instance running on a machine listens for incoming messages on a port.  All application
 * instances on a given machine must listen on a different port. 
 * - DEFAULT_PORT_NUMBER : PortNumber
 * -- Often there is only one application running on a machine.  It usually listens on this default port number.
 * -- The current value si 49200.
 * - portNumber : Integer RANGE 49152..65535
 * @author Scott Woodfield
 */
public class PortNumber {
	//The default port number that an application listens on.
	public final static PortNumber DEFAULT_PORT_NUMBER = generateDefaultPortNumber();
	//The actual port number the current application is listening on.
	private static PortNumber APPLICATIONS_PORT_NUMBER = null;
	
//Class Methods
	/**
	 * The setter for the class variable <i>APPLICATIONS_PORT_NUMBER</i>.  Should be
	 * called only once when an application starts up.
	 * @param portNumber the applications port number
	 */
	public static void setApplicationsPortNumber(PortNumber portNumber){
		if(APPLICATIONS_PORT_NUMBER == null){
			APPLICATIONS_PORT_NUMBER = portNumber;
		}
	}
	
	/**
	 * Gets the port number of the application
	 * @return the 
	 */
	public static PortNumber getApplicationsPortNumber(){
		if(APPLICATIONS_PORT_NUMBER == null){
			APPLICATIONS_PORT_NUMBER = DEFAULT_PORT_NUMBER;
		}
		return APPLICATIONS_PORT_NUMBER;
	}
	
//Domain Implementation
	/**
	 * The implementation of a PortNumber's domain.
	 */
	private int portNumber;

//Constructors
	/**
	 * Constructs a PortNumber given a valid port number value.
	 * 
	 * @pre MIN_PORT_NUMBER &le; portNumber &le; MAX_PORT_NUMBER
	 * @post this.portNumber = portNumber
	 */
	public PortNumber(int portNumber){
		assert portNumber >= MIN_PORT_NUMBER;
		assert portNumber <= MAX_PORT_NUMBER;
		
		this.portNumber = portNumber;
	}
	
	/**
	 * Copy constructor.
	 * 
	 * @param portNumber the port number we are going to make a copy of.
	 * 
	 * @pre portNumber &ne; null;
	 * @post this.portNumber = portNumber.portNumber
	 */
	public PortNumber(PortNumber portNumber){
		this.portNumber = portNumber.portNumber;
	}

//Queries
	/**
	 * The portNumber getter.
	 * 
	 * @pre <i>None</i>
	 * @post result = portNumber
	 */
	public int getValue() {
		return portNumber;
	}
	
	/**
	 * The equals operator of a PortNumber
	 * 
	 * @pre <i>None</i>
	 * @post result = o &ne; null AND o &isin; PortNumber AND o.portNumber = portNumber
	 */
	public boolean equals(Object o){
		return o != null && o instanceof PortNumber && ((PortNumber)o).portNumber == portNumber;
	}
	
	/**
	 * Creates the string representation of a PortNumber.
	 * 
	 * @pre <i>None</i>
	 * @post result = Integer.toString(portNumber)
	 */
	public String toString(){
		String result = Integer.toString(portNumber);
		return result;
	}
	
	/**
	 * The hashCode for a PortNumber.  
	 * 
	 * @pre <i>None</i>
	 * @post result = portNumber
	 */
	public int hashCode(){
		return portNumber;
	}
	
//Auxiliary Variables, Constants, and Methods
	//Constants
	/**
	 * The minimum legal value for a PortNumber.
	 */
	public final int MIN_PORT_NUMBER = 49152;
	
	/**
	 * The maximum legal value for a PortNumber.
	 */
	public final int MAX_PORT_NUMBER = 65535;

	//Methods
	/**
	 * Used to intitialize the DEFAULT_PORT_NUMBER
	 * 
	 * @pre <i>None</i>
	 * @post result = new PortNumber(49200)
	 */
	private static PortNumber generateDefaultPortNumber(){
		return new PortNumber(49200);
	}
}