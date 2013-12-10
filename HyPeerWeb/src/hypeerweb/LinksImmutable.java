package hypeerweb;

import java.io.ObjectStreamException;

/**
 * A Links object that does not serialize to a Proxy
 * @author ljnutal6
 */
public class LinksImmutable extends Links {
	public LinksImmutable(Links links){
		super(links.UID, links);
	}
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}	
}
