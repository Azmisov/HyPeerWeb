package communicator;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Uniquely identifies an object in an application on another machine
 * Holds the object's IPAddress, Port, and unique identifier (UID)
 * @author Scott Woodfield
 */
public class RemoteAddress{
	//The machineAddr of the RemoteAddress
	public final InetAddress ip;
	//The portNumber of the RemoteAddress
	public final PortNumber port;
	//The localObjectId of the RemoteAddress
	public final int UID;
	
	//CONSTRUCTORS
	/**
	 * Constructs a RemoteAddress from a unique identifier
 Uses localhost as the machine ip and the applications port number
	 * @param UID the unique ID of the object in the application of the other machine
	 */
	public RemoteAddress(int UID) {
		this(null, PortNumber.getApplicationsPortNumber(), UID);
	}
	/**
	 * Constructs a RemoteAddress from a given machine name, portNumber, and unique identifier
	 * @param machineName the name of the machine, may be an IP ip or domain name.
		pass null to use localhost as the machine ip
	 * @param portNumber the portNumber of the application the object is in.
	 * @param UID the unique id of the object in the application of the other machine
	 */
	public RemoteAddress(String machineName, PortNumber portNumber, int UID) {
		InetAddress address_temp = null;
		if (machineName != null){
			try {
				address_temp = InetAddress.getByName(machineName);
			} catch (UnknownHostException ex) {
				System.err.println("Communicator: Machine name is invalid, using localhost.");
				machineName = null;
			}
		}
		if (machineName == null){
			try{
			    address_temp = InetAddress.getLocalHost();
			} catch(UnknownHostException ex){
				System.err.println("Communicator Could not get address of localhost, using null.");
				address_temp = null;
			}
		}
		this.ip = address_temp;
		this.port = portNumber;
		this.UID = UID;
	}

	//QUERIES
	/**
	 * Returns true iff this RemoteAddress the the input parameter are in the same application on the same machine.
	 * @param raddr the RemoteAddress we are comparing against.
	 * @return true, if the objects are on the same machine
	 */
	public boolean onSameMachineAs(RemoteAddress raddr){
		return raddr.ip.equals(ip) &&
		       raddr.port.equals(port);
	}
	
	//CLASS OVERRIDES
	@Override
	public int hashCode(){
		long result = 0;
		if (ip != null)
			result += ip.hashCode();
		if (port != null)
			result += port.hashCode();
		result += UID;
		return (int) result;
	}
	@Override
	public boolean equals(Object object){
		boolean result = object != null && object instanceof RemoteAddress;
		if (result){
			RemoteAddress raddr = (RemoteAddress) object;
			
			result = ((ip == null && raddr.ip == null ) ||
					  (ip != null && ip.equals(raddr.ip)))
					 &&
			         ((port == null && raddr.port == null) ||
			          (port != null && port.equals(raddr.port)))
					 &&
					 (UID == raddr.UID);
		}
		return result;                 
	}
}