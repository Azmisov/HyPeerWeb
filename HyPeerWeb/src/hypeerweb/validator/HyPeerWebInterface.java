package hypeerweb.validator;

/**
 * The methods required to use the Validator.
 * 
 * @author Scott Woodfield
 */
public interface HyPeerWebInterface {
//Queries
    /**
     * Returns all of the nodes in the HyPeerWeb.  They must be sorted by webId, smallest first.
     * 
     * @pre none
     * @post result.length = |HyPeerWeb| AND every node in the HyPeerWeb is in the result AND the result is ordered by WebId where the first node is node 0.
     *       If the HyPeerWeb is empty it returns an empty array of length 0.
     */
    NodeInterface[] getOrderedListOfNodes();
    
    /**
     * Returns the node in the HyPeerWeb with result.getWebId() = webId.  If no such node exists return null.
     * 
     * @param webId The webId of the node we are trying to get.
     * @pre none
     * @post If there is a node n in the HyPeerWeb such that n.webId = webId then result = n, otherwise, result = null
     */
    NodeInterface getNode(int webId);

//Commands
    
//I/O methods
}
