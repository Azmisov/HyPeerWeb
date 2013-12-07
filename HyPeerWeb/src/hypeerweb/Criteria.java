package hypeerweb;

import communicator.Communicator;

/**
 * Criteria for whether a node is valid or not
 * This is called by Node.findValidNode()
 * @author isaac
 */
public class Criteria {
	public static enum Type{
		INSERT, DISCONNECT, NONEMPTY
	}
	
	/**
	 * Checks to see if the "friend" of the "origin" node fits some criteria
	 * @param origin the originating node
	 * @param friend a node connected to the origin within "level" neighbor connections
	 * @return a Node that fits the criteria, otherwise null
	 */
	public static Node check(Type type, Node origin, Node friend){
		switch (type){
			case INSERT:
				//Insertion point is always the lowest point within recurseLevel connections
				Node low = friend.L.getLowestLink();
				if (low != null && low.getHeight() < origin.getHeight())
					return low;
				return null;
			case DISCONNECT:
				/* Check all nodes out to "recurseLevel" for higher nodes
					Any time we find a "higher" node, we go up to it
					We keep walking up the ladder until we can go no farther
					We don't need to keep track of visited nodes, since visited nodes
					will always be lower on the ladder We also never want to delete
					from a node with children
				*/
				//Check for higher nodes
				Node high = friend.L.getHighestLink();
				if (high != null && high.getHeight() > origin.getHeight())
					return high;
				//Then go up to children, if it has any
				if (origin == friend){
					Node child = origin.L.getHighestNeighbor();
					if (child.getWebId() > origin.getWebId())
						return child;
				}
				return null;
			case NONEMPTY:
				Segment seg = friend instanceof SegmentProxy ? (SegmentProxy) friend : (Segment) friend;
				//Is this segment empty?
				return seg.isSegmentEmpty() ? null : friend;
		}
		return null;
	}
}
