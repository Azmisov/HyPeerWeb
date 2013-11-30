package communicator;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Uniquely identifies an object in an application on another machine
 * Holds the object's IPAddress, Port, and unique identifier (UID)
 * @author Scott Woodfield
 */
public class RemoteAddress{
	//Port number constants
	public static final int
		MIN_PORT = 1,
		MAX_PORT = 65535,
		DEFAULT_PORT = 49200;
	//The machineAddr of the RemoteAddress
	public InetAddress ip;
	public String ip_string;
	//The portNumber of the RemoteAddress
	public int port;
	//The localObjectId of the RemoteAddress
	public int UID;
	
	//CONSTRUCTORS
	/**
	 * Constructs a RemoteAddress from a unique identifier
	 * Uses localhost as the machine ip and the applications port number
	 * @param UID the unique ID of the object in the application of the other machine
	 */
	public RemoteAddress(int UID){
		setPortUID(Communicator.getAddress().port, UID);
		try {
			ip = InetAddress.getLocalHost();
			ip_string = ip.getHostAddress();
		} catch (UnknownHostException ex) {
			System.err.println("Could not resolve localhost address! This should never happen");
		}
	}
	/**
	 * Constructs a RemoteAddress from a given machine name, portNumber, and unique identifier
	 * @param machineName the name of the machine, may be an IP ip or domain name.
		pass null to use localhost as the machine ip
	 * @param portNumber the portNumber of the application the object is in.
	 *	port must be within MIN_PORT and MAX_PORT constants; use zero to leave unspecified
	 * @param UID the unique id of the object in the application of the other machine
	 * @throws java.lang.Exception
	 */
	public RemoteAddress(String machineName, int portNumber, int UID) throws Exception{
		setPortUID(portNumber, UID);
		ip = machineName == null ? InetAddress.getLocalHost() : InetAddress.getByName(machineName);
		ip_string = ip.getHostAddress();
	}
	private void setPortUID(int portNumber, int UID){
		assert(portNumber == 0 || (portNumber >= MIN_PORT && portNumber <= MAX_PORT));
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
		return raddr.ip.equals(ip) && raddr.port == port;
	}
	
	//CLASS OVERRIDES
	@Override
	public int hashCode(){
		long result = port + UID;
		if (ip != null)
			result += ip.hashCode();
		return (int) result;
	}
	@Override
	public boolean equals(Object object){
		boolean result = object != null && object instanceof RemoteAddress;
		if (result){
			RemoteAddress raddr = (RemoteAddress) object;
			
			result = ((ip == null && raddr.ip == null ) ||
					  (ip != null && ip.equals(raddr.ip)))
						&& port == raddr.port
						&& UID == raddr.UID;
		}
		return result;                 
	}
}