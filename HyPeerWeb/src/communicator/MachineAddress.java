package communicator;

import java.net.InetAddress;

/**
 * The IP address of this machine on the internet.<br>
 * <br>
 * <b>Class Domain:</b>
 * <div style="text-indent:25px">
 * machineAddress : InetAddress
 * </div>
 * @author Scott Woodfield
 */
public class MachineAddress {
//Class Domain
	/**
	 * The machineAddress.
	 */
	private static InetAddress machineAddress = null;
	
//Class Methods
	/**
	 * Sets the address of the current machine.
	 * @param a string representing the name of the current machine.  Can be a URL or IP address.
	 * @pre machineName &ne; null AND machineName is a valid IP address or URL
	 * @post machineAddress = InetAddress.getByName(machineName)
	 */
	public static void setMachineAddress(String machineName){
		assert MachineAddress.machineAddress == null;
		try{
			MachineAddress.machineAddress = InetAddress.getByName(machineName);
		}catch(Exception e){
			System.err.println("ERROR in MachineAddress::setMachineAddress(String):" +
					           "    Machine name is not a valid machineName");
			System.exit(1);
		}
	}
	
	/**
	 * Returns this machines InetAddress.  The return result is null if the machineAddress has not been set yet.
	 * @pre None
	 * @result = machineAddress
	 */
	public static InetAddress getThisMachinesInetAddress(){
		return machineAddress;
	}
}