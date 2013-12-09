/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import java.io.ObjectStreamException;
import java.util.ArrayList;

/**
 *
 * @author ljnutal6
 */
public class LinksImmutable extends Links {
	public LinksImmutable(Node f, Node sf, Node isf, ArrayList<Node> n, ArrayList<Node> sn, ArrayList<Node> isn)
	{
		super(f, sf, isf, n, sn, isn);
	}
	@Override
	public Object writeReplace() throws ObjectStreamException {
		//TODO: Figure out how to send real links in case of replacing node
		return this;
	}
			
	
}
