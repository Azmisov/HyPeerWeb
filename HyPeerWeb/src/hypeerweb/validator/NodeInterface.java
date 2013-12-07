package hypeerweb.validator;

/**
 * The methods that must be implemented by a node in order to use the Validator.
 * 
 * Domain:
 *         webId                           : int
 *         height                           : int
 *         neighbors                      : NodeInterface[]
 *         surrogateNeighbors           : NodeInterface[]
 *         inverseSurrogateNeighbors : NodeInterface
 *         fold                      : NodeInterface
 *         surrogateFold              : NodeInterface
 *         inverseSurrogateFold      : NodeInterface
 *         parent                    : NodeInterface
 * 
 * Invariants: Neither the neighbors, surrogateNeighbors, or inverseSurrogateNeighbors may be null (though they may be empty).
 *                The neighbors, surrogateNeighbors, and inverseSurrogateNeighbors must be sorted
 *                Must satisfy all of the constraints of the conceptual model.
 *                Conceptually the constraints of the conceptual model can be checked by invoking validator.validate(this) but
 *                this can only be done using a validator created from the hypeerWeb containing this node.
 * 
 * @author Scott Woodfield
 */
public interface NodeInterface extends Comparable<NodeInterface>{
//Queries
    /**
     * Returns the webId
     * @pre none
     * @post result = webId
     */
    int             getWebId();
    
    /**
     * Returns the height
     * @pre none
     * @post result = height
     */
    int             getHeight();
    
    /**
     * Returns the neighbors
     * @pre none
     * @post result = neighbors
     */
    NodeInterface[] getNeighbors();
    
    /**
     * Returns the surrogateNeighbors
     * @pre none
     * @post result = surrogateNeighbors
     */
    NodeInterface[] getSurrogateNeighbors();
    
    /**
     * Returns the inverseSurrogateNeighbors
     * @pre none
     * @post result = inverseSurrogateNeighbors
     */
    NodeInterface[] getInverseSurrogateNeighbors();
    
    /**
     * Returns the fold
     * @pre none
     * @post result = fold
     */
    NodeInterface   getFold();
    
    /**
     * Returns the surrogateFold
     * @pre none
     * @post result = surrogateFold
     */
    NodeInterface   getSurrogateFold();
    
    /**
     * Returns the inverseSurrogateFold
     * @pre none
     * @post result = inverseSurrogateFold
     */
    NodeInterface   getInverseSurrogateFold();
    
    /**
     * Returns the parent
     * @pre none
     * @post result = parent
     */
    NodeInterface   getParent();
    
    /**
     * The implementation of the equals method that overrides the one defined in the Object class.
     * 
     * @param o  The object we are going to compare this to.
     * 
     * @pre none
     * @post o == null => result = false AND
     *       ! o instanceof NodeInterface => result = false
     *       ELSE result = this.webId == o.webId AND this.height == o.height
     */
    boolean            equals(Object o);
    
    /**
     * The implementation of the hashCode method that overrides the one defined in the Object class.
     * 
     * @pre none
     * @post result =  an integer used for hashSets, hashTables, hashMaps, etc.  Assumes there is a semi-uniform  and random mapping from anything
     *       that implements NodeInterface to the integers in the Java type Integer.  Result is the integer an instance of NodeInterface would map to.
     */
    @Override
    int                hashCode();
    
    /**
     * The implementation of the compareTo method.  Used satisfy the interface Comparable<T>.  It is used to sort nodes and possibly in methods from the library.
     * 
     * @pre node != null
     * @post this.getWebId() < node.getWebId()  => result = -1 AND
     *       this.getWebId() == node.getWebId() => result = 0 AND
     *       this.getWebId() > node.getWebId()  => result = 1
     */
    int             compareTo(NodeInterface node);
    
//Commands
    
//I/O
}
