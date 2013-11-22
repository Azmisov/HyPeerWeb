package communicator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	//The machineAddr of the GlobalObjectId
	private InetAddress machineAddr;
	//The portNumber of the GlobalObjectId
	private PortNumber portNumber;
	//The localObjectId of the GlobalObjectId
	private LocalObjectId localObjectId;

	/**
	 * Constructs a GlobalObjectId from a given portNumber, and localObjectId
	 * Uses localhost as the machine address
	 * @param portNumber the portNumber of the application the object is in.
	 * @param localObjectId the localObjectId the object in the application of the other machine
	 */
	public GlobalObjectId(PortNumber portNumber, LocalObjectId localObjectId) {
		this(null, portNumber, localObjectId);
	}
	
	/**
	 * Constructs a GlobalObjectId from a given machine name, portNumber, and localObjectId
	 * @param machineName the name of the machine, may be an IP address or domain name.
	 *		pass null to use localhost as the machine address
	 * @param portNumber the portNumber of the application the object is in.
	 * @param localObjectId the localObjectId the object in the application of the other machine
	 */
	public GlobalObjectId(String machineName, PortNumber portNumber, LocalObjectId localObjectId) {
		if (machineName != null){
			try {
				machineAddr = InetAddress.getByName(machineName);
			} catch (UnknownHostException ex) {
				System.err.println("Communicator: Machine name is invalid, using localhost.");
				machineName = null;
			}
		}
		if (machineName == null){
			try{
			    machineAddr = InetAddress.getLocalHost();
			} catch(UnknownHostException ex){
				System.err.println("Communicator Could not get address of localhost, using null.");
				machineAddr = null;
			}
		}
		this.portNumber = portNumber;
		this.localObjectId = localObjectId;
	}
	
//Queries
	/**
	 * Returns the machineAddress as a string.
	 */
	public String getMachineAddr(){return machineAddr.getHostAddress();}
	
	/**
	 * portNumber getter.
	 */
	public PortNumber getPortNumber(){return portNumber;}
	
	/**
	 * localObjectId getter.
	 */
	public LocalObjectId getLocalObjectId(){return localObjectId;}
	
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
	@Override
	public int hashCode(){
		long result = 0;
		if (machineAddr != null)
			result += machineAddr.hashCode();
		if (portNumber != null)
			result += portNumber.hashCode();
		if (localObjectId != null)
			result += localObjectId.hashCode();
		return (int) result;
	}
	
	/**
	 * The equality operation.
	 * 
	 * @pre <i>None</i>
	 * @post result = object != null and object &isin; GlobalObjectId AND<br>
	 *                machineAddr = object.machineAddr AND portNumber = object.portNumber AND localObjectId = object.localObjectId
	 */
	@Override
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
}