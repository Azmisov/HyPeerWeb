/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hypeerweb;

import hypeerweb.validator.NodeInterface;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 *
 * @author isaac
 */
public class NodeCache implements NodeInterface, Serializable {
	//Network id
	protected int networkID;
	//Node attributes
	protected final int webID, height;
	//Node links
	protected final int[] n, sn, isn;
	protected int f = -1, sf = -1, isf = -1;
	protected transient SegmentCache parent;

	public NodeCache(hypeerweb.Node real, final SegmentCache parent){
		this.parent = parent;
		webID = real.getWebId();
		height = real.getHeight();
		Segment host = real.getHostSegment();
		networkID = host != null ? host.getWebId() : -1;
		//Folds
		hypeerweb.Node temp;
		if ((temp = real.L.getFold()) != null)
			f = temp.getWebId();
		if ((temp = real.L.getSurrogateFold()) != null)
			sf = temp.getWebId();
		if ((temp = real.L.getInverseSurrogateFold()) != null)
			isf = temp.getWebId();
		//Neighbors
		n = convertToCached(real.L.getNeighbors());
		sn = convertToCached(real.L.getSurrogateNeighbors());
		isn = convertToCached(real.L.getInverseSurrogateNeighbors());
	}

	//BASIC GETTERS
	public int getNetworkId() {
		return networkID;
	}
	@Override
	public int getWebId() {
		return webID;
	}
	@Override
	public int getHeight() {
		return height;
	}
	@Override
	public NodeCache getFold() {
		return parent.nodes.get(f);
	}
	public int getRawFold() {
		return f;
	}
	@Override
	public NodeCache getSurrogateFold() {
		return parent.nodes.get(sf);
	}
	public int getRawSurrogateFold() {
		return sf;
	}
	@Override
	public NodeCache getInverseSurrogateFold() {
		return parent.nodes.get(isf);
	}
	public int getRawInverseSurrogateFold() {
		return isf;
	}
	@Override
	public NodeCache[] getNeighbors() {
		return mapToCached(n);
	}
	public int[] getRawNeighbors() {
		return n;
	}
	@Override
	public NodeCache[] getSurrogateNeighbors() {
		return mapToCached(sn);
	}
	public int[] getRawSurrogateNeighbors() {
		return sn;
	}
	@Override
	public NodeCache[] getInverseSurrogateNeighbors() {
		return mapToCached(isn);
	}
	public int[] getRawInverseSurrogateNeighbors() {
		return isn;
	}

	//SPECIALIZED GETTERS
	@Override
	public NodeCache getParent() {
		//See comments in hypeerweb.Node class for documentation
		if (webID == 0)
			return null;
		int parID = webID & ~Integer.highestOneBit(webID);
		return Arrays.binarySearch(n, parID) < 0 ? null : parent.nodes.get(parID);
	}
	public NodeCache[] getTreeChildren() {
		//See comments in hypeerweb.Node class for documentation
		HashSet<Integer> generatedNeighbors = new HashSet();
		HashSet<Integer> generatedInverseSurrogates = new HashSet();
		ArrayList<NodeCache> found = new ArrayList();
		int id_surr = webID | ((1 << (height - 1)) << 1);
		int trailingZeros = Integer.numberOfTrailingZeros(webID);
		int bitShifter = 1;
		for (int i = 0; i < trailingZeros; i++) {
			generatedNeighbors.add(webID | bitShifter);
			generatedInverseSurrogates.add(id_surr | bitShifter);
			bitShifter <<= 1;
		}
		for (int id : n) {
			if (generatedNeighbors.contains(id))
				found.add(parent.nodes.get(id));
		}
		for (int id : isn) {
			if (generatedInverseSurrogates.contains(id))
				found.add(parent.nodes.get(id));
		}
		return found.toArray(new NodeCache[found.size()]);
	}
	public NodeCache getTreeParent() {
		//See comments in hypeerweb.Node class for documentation
		if (webID == 0)
			return null;
		int neighborID = webID & ~Integer.lowestOneBit(webID);
		int idx = Arrays.binarySearch(n, neighborID);
		if (idx < 0) {
			NodeCache temp;
			for (int snID : sn){
				temp = parent.nodes.get(snID);
				if (temp != null && temp.webID == (neighborID & ~((1 << (temp.height - 1)) << 1)))
					return temp;
			}
		}
		else return parent.nodes.get(n[idx]);
		return null;
	}

	private NodeCache[] mapToCached(int[] idList) {
		NodeCache[] cached = new NodeCache[idList.length];
		for (int i = 0; i < idList.length; i++)
			cached[i] = parent.nodes.get(idList[i]);
		return cached;
	}
	private int[] convertToCached(hypeerweb.Node[] realList) {
		int[] temp = new int[realList.length];
		for (int i = 0; i < realList.length; i++)
			temp[i] = realList[i].getWebId();
		Arrays.sort(temp);
		return temp;
	}

	@Override
	public int compareTo(NodeInterface node) {
		int id = node.getWebId();
		if (webID == id) {
			return 0;
		}
		int nh = node.getHeight();
		return (height == nh ? webID < id : height < nh) ? -1 : 1;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof NodeCache)) {
			return false;
		}
		return this.webID == ((NodeCache) obj).webID;
	}
	@Override
	public int hashCode() {
		return new Integer(this.webID).hashCode();
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\nNode: ").append(webID).append("(").append(height).append(")");
		//Folds
		if (f != -1) {
			builder.append("\n\tFold: ").append(f);
		}
		if (sf != -1) {
			builder.append("\n\tSFold: ").append(sf);
		}
		if (isf != -1) {
			builder.append("\n\tISFold: ").append(isf);
		}
		//Neighbors
		if (n.length > 0) {
			builder.append("\n\tNeighbors: ");
			for (int a_n : n) {
				builder.append(a_n).append(", ");
			}
		}
		if (sn.length > 0) {
			builder.append("\n\tSNeighbors: ");
			for (int a_sn : sn) {
				builder.append(a_sn).append(", ");
			}
		}
		if (isn.length > 0) {
			builder.append("\n\tISNeighbors: ");
			for (int a_isn : isn) {
				builder.append(a_isn).append(", ");
			}
		}
		return builder.toString();
	}
	
}
