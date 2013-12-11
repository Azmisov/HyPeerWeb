/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hypeerweb;

import java.io.Serializable;

/**
 *
 * @author Gangsta
 */
public class NodeImmutable implements Serializable{
	public final Node.FoldState foldState;
	public final int webID, height;
	public final LinksImmutable L;
	public final Attributes data;

	public NodeImmutable(Node node) {
		foldState = node.foldState;
		webID = node.webID;
		height = node.height;
		L = new LinksImmutable(node.L);
		data = node.data;
	}
}
