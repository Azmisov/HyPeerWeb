/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import java.io.ObjectStreamException;

/**
 *
 * @author Gangsta
 */
public class NodeImmutable extends Node {

	public NodeImmutable(Node node) {
		super(node.webID, node.height);
		L = new LinksImmutable(node.L);
	}
	@Override
	public Object writeReplace() throws ObjectStreamException {
		return this;
	}
    
}
