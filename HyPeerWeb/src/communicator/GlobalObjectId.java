package communicator;

import java.net.InetAddress;

/**
 * Uniquely identifies an object in an application on another machine.
 * 
 * <pre>
 * <b>Domain:</b>
 *     machineAddr   : InetAddress
 *     portNumber    : PortNumber -- of the application running on the indicated machine
 *     localObjectId : LocalObjectId
 *     
 *     <b>Invariant:</b>
 *     machineAddr &ne; null
 * </pre>
 * 
 * @author Scott Woodfield
 */
public class GlobalObjectId {
//Domain Implementation
	/**
	 * The machineAddr of the GlobalObjectId
	 */
	private InetAddress machineAddr;
	
	/**
	 * The portNumber of the GlobalObjectId
	 */
	private PortNumber portNumber;
	
	/**
	 * The localObjectId of the GlobalObjectId
	 */
	private LocalObjectId localObjectId;

//Constructors
	/**
	 * The default constructor.
	 * 
	 * @pre <i>None</i>
	 * @post machineAddress = MachineAddress.getThisMachinesInetAddress() AND
	 *       portNumber = PortNumber.getApplicationsPortNumber()          AND
	 *       localObjectId = new LocalObjectId()
	 */
	public GlobalObjectId(){
		machineAddr = MachineAddress.getThisMachinesInetAddress();
		portNumber = PortNumber.getApplicationsPortNumber();
		localObjectId = new LocalObjectId();
	}
	
	/**
	 * Copy Constructor.
	 * 
	 * @param globalObjectId the globalObjectId we will make a copy of.
	 * 
	 * @pre <i>globalObjectId &ne; null
	 * @post this.equals(globalObjectId)
	 */
	public GlobalObjectId(GlobalObjectId globalObjectId){
		machineAddr = globalObjectId.machineAddr;
		portNumber = new PortNumber(globalObjectId.portNumber);
		localObjectId = new LocalObjectId(globalObjectId.localObjectId);
	}
	
	/**
	 * Constructs a GlobalObjectId from a given machine name, portNumber, and localObjectId
	 * 
	 * @param machineName the name of the machine, may be an IP address or domain name.
	 * @param portNumber the portNumber of the application the object is in.
	 * @param localObjectId the localObjectId the object in the application of the other machine
	 * 
	 * @pre <i>None</i>
	 * @post valid machineName &rArr; this.machineAddr = InetAddress.getByName(machineName) AND<br>
	 *       &not; valid machineName AND &exist; localHost &rArr; machineName = InetAddress.getLocalHost() AND<br>
	 *       &not; valid machineName AND &not; &exist; localHost &rArr; machineName = null AND<br>
	 *       this.portNumber = portNumber AND this.localObjectId = localObjectId
	 */
	public GlobalObjectId(String machineName, PortNumber portNumber, LocalObjectId localObjectId) {
		try{
		    this.machineAddr = InetAddress.getByName(machineName);
		}catch(Exception e1){
			System.out.println("GlobalObjectId::GlobalObjectId(String, PortNumber, LocalObjectId):\n" +
					           "    ERROR: machine name is invalid, using localhost.");
			try{
			    this.machineAddr = InetAddress.getLocalHost();
			}catch(Exception e){
				System.out.println("GlobalObjectId::GlobalObjectId(String, PortNumber, LocalObjectId):\n" +
		           "    ERROR: could not get address of local host, using null.");
				this.machineAddr = null;
			}
		}
		this.portNumber = portNumber;
		this.localObjectId = localObjectId;
	}
	
//Queries
	/**
	 * Returns the machineAddress as a string.
	 * 
	 * @pre machineAddr &ne; null
	 * @post result = machineAddr.getHostAddress()
	 */
	public String getMachineAddr(){return machineAddr.getHostAddress();}
	
	/**
	 * portNumber getter.
	 * 
	 * @pre <i>None</i>
	 * @post result = portNumber
	 */
	public PortNumber getPortNumber(){return portNumber;}
	
	/**
	 * localObjectId getter.
	 * 
	 * @pre <i>None</i>
	 * @post result = localObjectId
	 */
	public LocalObjectId getLocalObjectId(){return localObjectId;}
	
	/**
	 * Converts a GlobalObjectId to its string representation.
	 * 
	 * @pre machineAddr &ne; null
	 * @post result = "GlobalObjectId: " + <br>
		              "Machine address = " + getMachineAddr() +", port number = " + portNumber + <br>
		              ", localObjectId = " + localObjectId;
	 */
	public String toString(){
		String result = "GlobalObjectId: " + 
		                "Machine address = " + getMachineAddr() +", port number = " + portNumber +
		                ", localObjectId = " + localObjectId;
		return result;
	}
	
	/**
	 * Returns true iff this globalObjectId the the input parameter are in the same application on the same machine.
	 * 
	 * @param globalObjectId the globalObjectId we are comparing against.
	 * 
	 * @pre globalObjectId &ne; null
	 * @post result = globalObjectId.machineAddr = machineAddr AND globalObjectId.portNumber = portNumber
	 */
	public boolean onSameMachineAs(GlobalObjectId globalObjectId){
		return globalObjectId.machineAddr.equals(machineAddr) &&
		       globalObjectId.portNumber.equals(portNumber);
	}
	
	/**
	 * The hashCode for a globalObjectId.
	 * 
	 * @pre <i>None</i>
	 * @post machAddr &ne; null &rArr;<br>
	 *           portNumber &ne; null &rArr;<br>
	 *               localObjectId &ne; null &rArr;
	 *                   result = (int)((long)machineAddr.hashCode() + (long)portNumber.hashCode() + (long)localObjectId.hashCode())
	 *               ELSE
	 *                   result = (int)((long)machineAddr.hashCode() + (long)portNumber.hashCode())
	 *           ELSE
	 *               localObjectId &ne; null &rArr;
	 *                   result = (int)((long)machineAddr.hashCode() + (long)localObjectId.hashCode())
	 *               ELSE
	 *                   result = machineAddr.hashCode()
	 *       machAddr = null &rArr;<br>
	 *       	 portNumber &ne; null &rArr;<br>
	 *               localObjectId &ne; null &rArr;
	 *                   result = (int)((long)portNumber.hashCode() + (long)localObjectId.hashCode())
	 *               ELSE
	 *                   result = portNumber.hashCode()
	 *           ELSE
	 *               localObjectId &ne; null &rArr;
	 *                   result = localObjectId.hashCode()
	 *               ELSE
	 *                   result = 0
	 */
	public int hashCode(){
		long result = 0l;
		if(machineAddr != null){
			result += machineAddr.hashCode();
		}
		if(portNumber != null){
			result += portNumber.hashCode();
		}
		
		if(localObjectId != null){
			result += localObjectId.hashCode();
		}
		
		return (int)result;
	}
	
	/**
	 * The equality operation.
	 * 
	 * @pre <i>None</i>
	 * @post result = object != null and object &isin; GlobalObjectId AND<br>
	 *                machineAddr = object.machineAddr AND portNumber = object.portNumber AND localObjectId = object.localObjectId
	 */
	public boolean equals(Object object){
		boolean result = object != null && object instanceof GlobalObjectId;
		if(result){
			GlobalObjectId globalObjectId = (GlobalObjectId)object;
			
			result = ((machineAddr == null && globalObjectId.machineAddr == null ) ||
					  (machineAddr != null && machineAddr.equals(globalObjectId.machineAddr))
					 )
					 &&
			         ((portNumber == null && globalObjectId.portNumber == null) ||
			          (portNumber != null && portNumber.equals(globalObjectId.portNumber))
			         )
			         &&
			         ((localObjectId == null && globalObjectId.localObjectId == null) ||
			          (localObjectId != null && localObjectId.equals(globalObjectId.localObjectId))
			         );
		}
		return result;                 
	}
	
//Auxiliary Constants, Variables, and Methods
	//private InetAddress getThisMachineAddress(){
	//	return null;
	//}
}