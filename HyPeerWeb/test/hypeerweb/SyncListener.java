package hypeerweb;

import communicator.NodeListener;

/**
 * Overrides NodeListener to be synchronous
 * @author isaac
 */
public class SyncListener extends NodeListener{
	public SyncListener(){
		this(null, null);
	}
	public SyncListener(String cname, String mname) {
		super(cname, mname);
	}
	@Override
	public void callback(Node n){}
}
