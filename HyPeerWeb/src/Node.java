
/**
 *
 * @author Guy int and Integer need to be replaced with WebID and Height when
 * those types exist.
 */
import java.util.ArrayList;

public class Node {
	//NODE ATTRIBUTES

	private int webID;
	private int height;
	private int fold;
	private int surrogateFold;
	private int inverseSurrogateFold;
	private ArrayList<Integer> neighbors = new ArrayList();
	private ArrayList<Integer> surrogateNeighbors = new ArrayList();
	private ArrayList<Integer> inverseSurrogateNeighbors = new ArrayList();

	//CONSTRUCTORS
	/**
	 * Create a Node with only a WebID
	 *
	 * @param id The WebID of the Node
	 */
	public Node(int id, int height) {
		webID = id;
		this.height = height;
	}

	/**
	 * Create a Node with all of its data
	 *
	 * @param id The WebID of the Node
	 * @param Height The Height of the Node
	 * @param Fold The Fold of the Node
	 * @param sFold The Surrogate Fold of the Node
	 * @param isFold The Inverse Surrogate Fold of the Node
	 * @param Neighbors An ArrayList containing the Neighbors of the Node
	 * @param sNeighbors An ArrayList containing the Surrogate Neighbors of the
	 * Node
	 * @param isNeighbors An ArrayList containing the Inverse Surrogate
	 * Neighbors of the Node
	 */
	public Node(int id, int Height, int Fold, int sFold, int isFold,
			ArrayList<Integer> Neighbors, ArrayList<Integer> sNeighbors,
			ArrayList<Integer> isNeighbors) {
		webID = id;
		height = Height;
		fold = Fold;
		surrogateFold = sFold;
		inverseSurrogateFold = isFold;

		if (Neighbors != null) {
			neighbors = Neighbors;
		}
		if (sNeighbors != null) {
			surrogateNeighbors = sNeighbors;
		}
		if (isNeighbors != null) {
			inverseSurrogateNeighbors = isNeighbors;
		}
	}
	
	/**
	 * Finds the closest valid insertion point (the parent
	 * of the child to add) from a starting node
	 * @return the parent of the child to add
	 * @author josh
	 */
	public Node findInsertionNode(){
		return null;
	}
	

	//WEBID
	/**
	 * Gets the WebID of the Node
	 *
	 * @return The WebID of the Node
	 */
	public int getWebID() {
		return webID;
	}

	//HEIGHT
	/**
	 * Gets the Height of the Node
	 *
	 * @return The Height of the Node
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Sets the Height of the Node
	 *
	 * @param h The Height
	 */
	public void setHeight(int h) {
		height = h;
	}

	//FOLD
	/**
	 * Gets the WebId of the Node's Fold
	 *
	 * @return The WebID of the Node's Fold
	 */
	public int getFold() {
		return fold;
	}

	/**
	 * Sets the WebID of the Fold of the Node
	 *
	 * @param f The WebID of the Fold of the Node
	 */
	public void setFold(int f) {
		fold = f;
	}

	//SURROGATE FOLD
	/**
	 * Gets the WebID of the Surrogate Fold of the Node
	 *
	 * @return The WebID of the Surrogate Fold of the Node
	 */
	public int getSurrogateFold() {
		return surrogateFold;
	}

	/**
	 * Sets the WebID of the Surrogate Fold of the Node
	 *
	 * @param sf The WebID of the Surrogate Fold of the Node
	 */
	public void setSurrogateFold(int sf) {
		surrogateFold = sf;
	}

	//INVERSE SURROGATE FOLD
	/**
	 * Gets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @return The WebID of the Inverse Surrogate Fold of the Node
	 */
	public int getInverseSurrogateFold() {
		return inverseSurrogateFold;
	}

	/**
	 * Sets the WebID of the Inverse Surrogate Fold of the Node
	 *
	 * @param sf The WebID of the Inverse Surrogate Fold of the Node
	 */
	public void setInverseSurrogateFold(int sf) {
		inverseSurrogateFold = sf;
	}

	//NEIGHBORS
	/**
	 * Gets an ArrayList containing the Neighbors of the Node
	 *
	 * @return An ArrayList containing the Neighbors of the Node
	 */
	public ArrayList<Integer> getNeighbors() {
		return neighbors;
	}

	/**
	 * Replaces the list of Neighbors with a new list
	 *
	 * @param al An ArrayList containing the new list of Neighbors. If al is
	 * null nothing will be changed
	 */
	public void setNeighbors(ArrayList<Integer> al) {
		if (al != null) {
			neighbors = al;
		}
	}

	/**
	 * Adds a Neighbor WebID to the list of Neighbors if it is not already in
	 * the list
	 *
	 * @param n The WebID of the Neighbor
	 */
	public void addNeighbor(int n) {
		if (!isNeighbor(n)) {
			neighbors.add(n);
		}
	}

	/**
	 * Checks to see if a WebID is in the list of Neighbors
	 *
	 * @param n The WebID to check
	 * @return True if found, false otherwise
	 */
	public boolean isNeighbor(int n) {
		return neighbors.contains(n);
	}

	/**
	 * Deletes a WebID if it is in the list of Neighbors
	 *
	 * @param n The WebID to delete
	 */
	public void deleteNeighbor(int n) {
		neighbors.remove(n);
	}

	//SURROGATE NEIGHBORS
	/**
	 * Gets an ArrayList containing the Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Surrogate Neighbors of the Node
	 */
	public ArrayList<Integer> getSurrogateNeighbors() {
		return surrogateNeighbors;
	}

	/**
	 * Replaces the list of Surrogate Neighbors with a new list
	 *
	 * @param al An ArrayList containing the new list of Surrogate Neighbors If
	 * al is null nothing will be changed
	 */
	public void setSurrogateNeighbors(ArrayList<Integer> al) {
		if (al != null) {
			surrogateNeighbors = al;
		}
	}

	/**
	 * Adds a Surrogate Neighbor WebID to the list of Surrogate Neighbors if it
	 * is not already in the list
	 *
	 * @param sn The WebID of the Surrogate Neighbor
	 */
	public void addSurrogateNeighbor(int sn) {
		if (!isSurrogateNeighbor(sn)) {
			surrogateNeighbors.add(sn);
		}
	}

	/**
	 * Checks to see if a WebID is in the list of Surrogate Neighbors
	 *
	 * @param sn The WebID to check
	 * @return True if found, false otherwise
	 */
	public boolean isSurrogateNeighbor(int sn) {
		return surrogateNeighbors.contains(sn);
	}

	/**
	 * Deletes a WebID if it is in the list of Surrogate Neighbors
	 *
	 * @param sn The WebID to delete
	 */
	public void deleteSurrogateNeighbor(int sn) {
		surrogateNeighbors.remove(sn);
	}

	//INVERSE SURROGATE NEIGHBORS
	/**
	 * Gets an ArrayList containing the Inverse Surrogate Neighbors of the Node
	 *
	 * @return An ArrayList containing the Inverse Surrogate Neighbors of the
	 * Node
	 */
	public ArrayList<Integer> getInverseSurrogateNeighbors() {
		return inverseSurrogateNeighbors;
	}

	/**
	 * Replaces the list of Inverse Surrogate Neighbors with a new list
	 *
	 * @param al An ArrayList containing the new list of Inverse Surrogate
	 * Neighbors. If al is null nothing will be changed
	 */
	public void setInverseSurrogateNeighbors(ArrayList<Integer> al) {
		if (al != null) {
			inverseSurrogateNeighbors = al;
		}
	}

	/**
	 * Adds an Inverse Surrogate Neighbor WebID to the list of Inverse Surrogate
	 * Neighbors if it is not already in the list
	 *
	 * @param isn The WebID of the Inverse Surrogate Neighbor
	 */
	public void addInverseSurrogateNeighbor(int isn) {
		if (!isInverseSurrogateNeighbor(isn)) {
			inverseSurrogateNeighbors.add(isn);
		}
	}

	/**
	 * Checks to see if a WebID is in the list of Inverse Surrogate Neighbors
	 *
	 * @param isn The WebID to check
	 * @return True if found, false otherwise
	 */
	public boolean isInverseSurrogateNeighbor(int isn) {
		return inverseSurrogateNeighbors.contains(isn);
	}

	/**
	 * Deletes a WebID if it is in the list of Inverse Surrogate Neighbors
	 *
	 * @param isn The WebID to delete
	 */
	public void deleteInverseSurrogateNeighbor(int isn) {
		inverseSurrogateNeighbors.remove(isn);
	}
        
        /**
         * Finds and returns the node whose WebID is closest to the given long
         * Assumed to always start with the node with WebID of zero
         * @param index The value to get as close as possible to
         */
        public Node findStartPointForInsertion(long index)
        {
            long closeness = index & webID;
            for (int i=0; i < neighbors.size(); i++)
            {
                long c = index & neighbors.get(i);
            }
            return this;
        }
}
