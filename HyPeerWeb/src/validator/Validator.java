package validator;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Use to validate all of the nodes in a HyPeerWeb as to whether they satisfy the constraints of the conceptual model of a HyPeerWeb.
 * Requires the hypeerWeb to satisfy the specification of a HyPeerWebInterface.
 * Requires that all nodes satisfy the specification of the NodeInterface
 * 
 * Domain: hypeerWeb : HyPeerWebInterface
 *            nodes     : List<NodeInterface>
 * 
 * Invariants: hypeerWeb AND nodes are not null.
 *             All nodes in the hypeerWeb are contained in nodes.  Every node in nodes must be in the hypeerWeb.
 * 
 * @author Scott Woodfield
 */
public class Validator {
//Domain Implementation
    /**
     * The HyPeerWeb to be checked.
     */
    private HyPeerWebInterface hypeerWeb = null;
    
    /**
     * All nodes (represented as an array) from the hypeerWeb.
     */
    private NodeInterface[] nodes = null;
    
//Constructors
    /**
     * The Constructor for the Validator.
     * Though it is not required, this implementation initializes the nodes attribute with an ordered list.  Because of this, when we process the
     * the list of nodes, it will be done from smallest to largest.  This is helpful during debugging and reading a log file.
     * 
     * @param hypeerWeb The HyPeerWeb we are constructing a Validator for.
     * 
     * @pre hypeerWeb != null
     * @post this.hypeerWeb == hypeerWeb AND
     *       nodes contains all and only the nodes contained in the hypeerWeb
     */
    public Validator(HyPeerWebInterface hypeerWeb) {
        this.hypeerWeb = hypeerWeb;
        this.nodes = hypeerWeb.getOrderedListOfNodes();
    }
    
//Queries
    /**
     * Ensures that every node in the hypeerWeb satisfies the constraints found in the conceptual model.
     * 
     * @pre none
     * @post result = true if and only if forAll node in HyPeerWeb (validateNode(node)
     */
    public boolean validate() {
        boolean valid = true;
        
        for(NodeInterface node : nodes) {
            valid = valid && validateNode(node);
        }
        
        if(!valid) {
            System.out.println("\nNodes in HyPeerWeb of size " + nodes.length + ":");
            for(NodeInterface node : nodes) {
                System.out.println("WebId: " + node.getWebId() + 
                        " Height: " + node.getHeight() + 
                        " Fold: " + node.getFold() +
                        " SFold: " + node.getSurrogateFold() + 
                        " ISFold: " + node.getInverseSurrogateFold());
                System.out.print("Ns: ");
                NodeInterface[] neighbors = node.getNeighbors();
                for(NodeInterface n : neighbors){
                    System.out.print(n.getWebId() + " ");
                }
                System.out.print(" SNs: ");
                NodeInterface[] sneighbors = node.getSurrogateNeighbors();
                for(NodeInterface n : sneighbors){
                    System.out.print(n.getWebId() + " ");
                }
                System.out.print(" ISNs: ");
                NodeInterface[] isneighbors = node.getInverseSurrogateNeighbors();
                for(NodeInterface n : isneighbors){
                    System.out.print(n.getWebId() + " ");
                }
                System.out.println();
		System.out.println();
            }
            System.out.println();
        }

        return valid;
    }
    
    /**
     * Checks the given node to determine whether it satisfies all the constraints on the conceptual HyPeerWeb Model.
     * 
     * @param node The node to be checked
     * 
     * @pre node != null
     * @post result = true iff the nodes satisfies all of the constraints in the conceptual model
     */
    public boolean validateNode(NodeInterface node) {
        int totalNeighborsAndSurrogateNeighbors = node.getNeighbors().length + node.getSurrogateNeighbors().length;
        boolean hasFoldOrSurrogateFold = (node.getFold() != null) || (node.getSurrogateFold() != null);
        
        boolean validationFailed = false;
        
        //CONSTRAINT: If |HyPeerWeb| == 1 then that single node must be node 0.
        if(nodes.length == 1 && nodes[0].getWebId() != 0) {
            validationFailed = printErrorMessage(validationFailed, node, 
                    "    If |hypeerWeb| = 1 then the single node in the hypeerWeb must be node 0");
            return validationFailed;
        }
        
        //CONSTRAINT: The webId of every node must be >= 0
        if(node.getWebId() < 0) {
            validationFailed = printErrorMessage(validationFailed, node, 
                    "    A node should have a webId >= 0 but this one is " + node.getWebId());
            return validationFailed;
        }
        
        //CONSTRAINT: The height of every node must be >= 0
        if(node.getHeight() < 0) {
            validationFailed = printErrorMessage(validationFailed, node, 
                    "    A node should have a height >= 0 but this one is " + node.getHeight());
            return validationFailed;
        }

        //CONSTRAINT: The height of every node must equal the number of its neighbors + the number of its surrogate neighbors
        if(totalNeighborsAndSurrogateNeighbors != node.getHeight()) {
            validationFailed = printErrorMessage(validationFailed, node, 
                "    Expected " + node.getHeight() + " neighbors and surrogate neighbors, but found " + totalNeighborsAndSurrogateNeighbors);
        }
        
        //CONSTRAINT: If a HyPeerWeb has a single node, n (which must be node 0), that node cannot have any
        //            fold, surrogate fold, or inverse surrogateFold
        if(node.getWebId() == 0 && node.getHeight() == 0) {
            if(hasFoldOrSurrogateFold || totalNeighborsAndSurrogateNeighbors != 0 || node.getSurrogateFold() != null) {
                validationFailed = printErrorMessage(validationFailed, node,
                    "    If a HyPeerWeb has a single node, node 0, it should not have neighbors, surrogate neighbors, a fold, a surrogate fold, or an inverse surrogate fold");
            }
        } else {
        //CONSTRAINT: If a HyPeerWeb contains 2 or more nodes, each must have at least one neighbor and a fold or surrogate fold.
            if(!hasFoldOrSurrogateFold || node.getNeighbors().length <= 0) {
                validationFailed = printErrorMessage(validationFailed, node,
                    "    Expected this node to have either a fold or surrogate fold but it has neither");
            }
        }
        
        //CONSTRAINT: Every node but node 0 must have a parent.  Node 0 may have no parent.
        NodeInterface parent = node.getParent();
        if(node.getWebId() > 0) {
            if(parent == null) {
                validationFailed = printErrorMessage(validationFailed, node,
                        "    Since this node is > 0, it was expected that it have a parent, but it did not");
            }
        } else if(node.getWebId() == 0) {
            if(parent != null) {
                validationFailed = printErrorMessage(validationFailed, node,
                        "    Since this node is node 0, it was expected that it not have a parent, but it did, the node " + parent.getWebId());
            }
        }
        
        //CONSTRAINT: A node may have SurrogateNeighbors or InverseSurrogateNeighbors but not both
        NodeInterface[] surrogateNeighbors = node.getSurrogateNeighbors();
        NodeInterface[] inverseSurrogateNeighbors = node.getInverseSurrogateNeighbors();
        if(surrogateNeighbors.length > 0 && inverseSurrogateNeighbors.length > 0 ) {
            if(parent != null) {
                validationFailed = printErrorMessage(validationFailed, node,
                        "    A node may have surrogate neighbors or inverse surrogate neighbors but not both");
            }
        }
        
                
        validationFailed = checkNeighborConstraints(node, validationFailed);

        validationFailed = checkSurrogateNeighborConstraints(node, validationFailed);
        
        validationFailed = checkFoldConstraints(node, validationFailed);
        
        validationFailed = checkSurrogateFoldConstraints(node, validationFailed);
        
        validationFailed = checkInverseSurrogateFoldConstraints(node, validationFailed);
        
        validationFailed = checkNeighborsNeighborsConstraints(node, validationFailed);

        return !validationFailed;
    }

//Commands
    
// Non-Public Attributes and Methods
    /**
     * Check to see if all the constraints on this node's neighbors hold.
     * 
     * @param node The node to be checked.
     * @param validationFailed Used to determine whether to print the error header for this node.
     * 
     * @pre node != null
     * @post returns true =>
     *             |hypeerWeb| = 1 => node (which must be node 0) has no neighbors AND
     *             every neighbor's height was within 1 of this node's height AND
     *           every neighbor's binary representation of its webId differed by exactly one from the binary representation of this node's webId
     *      ELSE
     *           return false
     */
    private boolean checkNeighborConstraints(NodeInterface node, boolean validationFailed) {
        NodeInterface[] neighbors = node.getNeighbors();
        
        //CONSTRAINT: |hypeerWeb| = 1 => this node (node 0) must have 0 neighbors.
        if(nodes.length == 1) {//Then this node must be node 0
            if(neighbors.length > 0) {
                validationFailed = printErrorMessage(validationFailed, node,
                        "    Since the HyPeerWeb has a single node (which must be node 0), it cannot have any neighbors");
            }
        }

        boolean atLeastOneNeighborsHeightWasHeigher = false;
        boolean atLeastOneNeighborsHeightWasLower = false;
        for(NodeInterface neighbor : neighbors) {
            int nodesHeight = node.getHeight();
            int neighborsHeight = neighbor.getHeight();
            int heightDifference = nodesHeight - neighborsHeight;
            boolean foundHeightError = false;
            
            //CONSTRAINT: Every neighbor's height must be the same as this node's height or, if at least one is one larger,
            //            all neighbors that are not the same must all be one larger
            if(heightDifference == 1) {
                if(atLeastOneNeighborsHeightWasHeigher && !foundHeightError) {
                    validationFailed = printErrorMessage(validationFailed, node,
                            "    This neighbor, " + neighbor.getWebId() + ", has a height of " + neighborsHeight + " which is one less than the nodes.\n" +
                            "    However, no other neighbors before this one had a lower height and at least one had a higher height. Every neighbor's height\n" +
                            "    must be the same as this node's height or, if at least one is one larger, all neighbors that are not the same must all be one larger");
                }
                foundHeightError = true;
                atLeastOneNeighborsHeightWasLower = true;
            
            //CONSTRAINT: The height of every neighbor must be at most one less than this node's height.
            } else if(heightDifference > 1) {
                validationFailed = printErrorMessage(validationFailed, node,
                        "    The height of every neighbor must be at most one less than this node's height.  Neighbor " + neighbor + " has height " + neighborsHeight);
            
            //CONSTRAINT: Every neighbor's height must be the same as this node's height, or, if at least one is one smaller,
            //            all neighbors that are not the same must all be smaller.
            } else if(heightDifference == -1) {
                if(atLeastOneNeighborsHeightWasLower && !foundHeightError) {
                    validationFailed = printErrorMessage(validationFailed, node,
                            "    This neighbor, " + neighbor.getWebId() + ", has a height of " + neighborsHeight + " which is one more than the nodes.\n" +
                            "    However, no other neighbors before this one had a higher height and at least one had a lower height. Every neighbor's height\n" +
                            "    must be the same as this node's height or, if at least one is one lower, all neighbors that are not the same must all be one lower");
                }
                foundHeightError = true;
                atLeastOneNeighborsHeightWasHeigher = true;
                
            //CONSTRAINT: The height of every neighbor must be at most one larger than this node's height.
            } else if(heightDifference < -1) {
                validationFailed = printErrorMessage(validationFailed, node,
                        "    The height of every neighbor must be at most one more than this node's height.  Neighbor " + neighbor + " has height " + neighborsHeight);
            }
            
            //CONSTRAINT: The binary representation of every neighbor of this node must differ from  this nodes binary representation by exactly one bit 
            int distanceToNeighbor = distanceTo(node, neighbor);
            if(distanceToNeighbor != 1) {
                validationFailed = printErrorMessage(validationFailed, node,
                    "    Expected the distance to neighbor, " + neighbor + ", with height " + neighbor.getHeight() + " to have a distance of 1 but had the distance " +
                    distanceToNeighbor);
            }
        }
        
        return validationFailed;
    }
    

    /**
     * Check a node to see if this node satisfies all constraints on its surrogate neighbors.
     * 
     * @param node The node to be checked.
     * @param validationFailed Used to determine whether to print the error header for errors.
     * 
     * @pre node != null
     * @post result = true =>
     *             |hypeerWeb| = 1 => node (which must be node 0) has no surrogate neighbors AND
     *             every surrogate neighbors height must be the same height as this node's or at most 1 smaller AND
     *           every surrogate neighbor's binary representation of its webId differs by exactly two bits from the binary representation of this node's webId AND
     *           every surrogate neighbor's binary representation of its webId differs from the binary representation of node's webId in the highest order one bit of node's binary representation
     *      ELSE
     *           result = false
     */
    private boolean checkSurrogateNeighborConstraints(NodeInterface node, boolean validationFailed) {
        NodeInterface[] surrogateNeighbors = node.getSurrogateNeighbors();
        if(nodes.length == 1) {
        //CONSTRAINT: In a hypeerWeb with exactly one node, that node (which is node 0) must have no surrogate neighbors.
            if(surrogateNeighbors.length > 0) {
                validationFailed = printErrorMessage(validationFailed, node,
                    "    If |hypeerWeb| = 1 then node 0 should have no surrogate neighbors but it has " + surrogateNeighbors.length + " surrogate neighbors");    
            }
        } else {
            for(NodeInterface surrogateNeighbor : surrogateNeighbors) {        
                //CONSTRAINT: If node s is the surrogate neighbor of n then n's height must be the same or at most one larger than s's height.
                if(node.getHeight() > surrogateNeighbor.getHeight() + 1 || node.getHeight() < surrogateNeighbor.getHeight()) {
                    validationFailed = printErrorMessage(validationFailed, node,
                        "    Node " + node.getWebId() + "'s height should be the same as or at most 1 higher than the surrogate neighbor " +
                            surrogateNeighbor.getWebId() + "(" + surrogateNeighbor.getHeight() + ")");
                }
                
                //CONSTRAINT: If node s is a surrogate neighbor of some node n then their corresponding binary representations must differ in exactly two places.
                int distanceToNeighbor = distanceTo(node, surrogateNeighbor);
                if(distanceToNeighbor != 2) {
                    if(!validationFailed) {
                        validationFailed = true;
                        System.err.println("VALIDATION FAILED for node " + node.getWebId() + " with height " + node.getHeight());
                    }
                    System.err.println("    Expected the distance to surrogate Neighbor, " + surrogateNeighbor.getWebId() + ", with height " +
                        surrogateNeighbor.getHeight() + " to have a distance of 2 but had the distance " + distanceToNeighbor);
                }
                
                //CONSTRAINT: If node s is a surrogate neighbor of n then if n' = n with it highest order one bit flipped to 0, then n' = s
                int positionOfHighestOrderOneBit = positionOfHighestOrderOneBit(node);
                if(node.getWebId() != 0) {
                    long mask = 1l << positionOfHighestOrderOneBit;
                    if((surrogateNeighbor.getWebId() & mask) != 0) {
                        validationFailed = printErrorMessage(validationFailed, node,
                            "    Expected the surrogate neighbor " + surrogateNeighbor + "'s bit at position " + positionOfHighestOrderOneBit +
                            "(postion of node's highest order one's bit) to be 0, but was not");
                    }
                }
            }
        }
        
        return validationFailed;
    }

    /**
     * Check a node to see if all of the constraints on a possibly associated fold hold
     *
     * @param node The node to be checked.
     * @param validationFailed Used to determine whether to print the error header for errors.
     * 
     * @pre node != null
     * @post result = true =>
     *             If the |hypeerWeb| = 1 then the single node in the hypeerWeb (node 0) cannot have a fold AND
     *             If n has a height h but there does not exist another node whose height is h and whose complement = n then n cannot have a fold AND
     *             If node n has a fold f and n.height = f.height then the webIds must be complements of each other AND
     *             If node n has a fold f and n.height = f.height + 1 (n's height is exactly one higher than f's height) then AND
     *                n must = the complement of fold's webId with with an added leading 0 to make them of the same height AND
     *            If node n has a fold f and n.height + 1 = f.height (n's height is exactly one less than f's height) then AND
     *                f must = the complement of n's webId with with an added leading 0 to make them of the same height AND
     *            If a node n has a fold f then n's height should differ from f's height by at most 1 AND
     *             If there exists a node in the hypeerWeb that has the same height as n and their webIds are complements of each other OR
     *               there exists a node f with height one less than n's and f's webId padded to the left with one 0 is the complement of n's webId OR
     *               there exists a node f with height one more than n's and n's webId padded to the left with one 0 is the complement of f's webId
     *             THEN n must have a fold
     *       ELSE
     *               result = false
     */
    private boolean checkFoldConstraints(NodeInterface node, boolean validationFailed) {
        if(nodes.length == 1) {  //|hypeerWeb| = 1
            //CONSTRAINT: If the |hypeerWeb| = 1 then this node (which must be node 0) must have no Fold
            if(node.getFold() != null) {
                validationFailed = printErrorMessage(validationFailed, node,
                    "    If the |hypeerWeb| == 1 then the single node in the hypeerWeb, node 0, should have no fold but has " + node.getFold().getWebId() + " as a fold"); 
            }
        } else {
            NodeInterface expectedFold = getFold(node);
            NodeInterface fold = node.getFold();
            if(node.getFold() != null) {
                //CONSTRAINT: A node n cannot have a fold if n has a height h but there does not exist another node whose height is h and whose complement = n
                if(expectedFold == null) {
                    validationFailed = printErrorMessage(validationFailed, node,
                        "    Node " + node.getWebId() + "(" + Long.toBinaryString(node.getWebId()) + " should not have a fold but has the fold " + fold.getWebId() +
                        "(" + Long.toBinaryString(fold.getWebId()) + ")");

                //CONSTRAINT: If node n has a fold f and n.height = f.height then the webIds must be complements of each other    
                } else if(node.getHeight() == fold.getHeight()) {
                    if(node.getWebId() != getComplement(fold, fold.getHeight())) {
                        validationFailed = printErrorMessage(validationFailed, node,
                            "    Node " + node.getWebId() + "(" + Long.toBinaryString(node.getWebId()) + ")'s fold is " + fold.getWebId() + "(" +
                            Long.toBinaryString(fold.getWebId()) + "). The fold should be the complement of the node");
                    }
                    
                //CONSTRAINT: If node n has a fold f and n.height = f.height + 1 (n's height is exactly one higher than f's height) then
                //            n must = the complement of fold's webId with with an added leading 0 to make them of the same height
                } else if(node.getHeight() == fold.getHeight() + 1) {
                    if(node.getWebId() != hypeerWeb.getNode(getComplement(fold, fold.getHeight() + 1)).getWebId()) {
                        validationFailed = printErrorMessage(validationFailed, node,
                            "    Node " + node.getWebId() + "(" + Long.toBinaryString(node.getWebId()) + ") with height " + node.getHeight() + " has the fold " + fold.getWebId() +
                            " with a height that is one lower(" + Long.toBinaryString(fold.getWebId()) + "). The fold should be the complement of the node");
                    }
                    
                //CONSTRAINT: If node n has a fold f and n.height + 1 = f.height (n's height is exactly one less than f's height) then
                //            f must = the complement of n's webId with with an added leading 0 to make them of the same height
                } else if(node.getHeight() + 1 == fold.getHeight()) {
                    if(hypeerWeb.getNode(getComplement(node,node.getHeight() + 1)).getWebId() != fold.getWebId()) {
                        validationFailed = printErrorMessage(validationFailed, node,
                                "    Node " + node.getWebId() + "(00" + Long.toBinaryString(node.getWebId()) + ") with height " + node.getHeight() + " has the fold " + fold + " with a height that is one higher(" + Long.toBinaryString(fold.getWebId()) +
                                "). The fold should be the complement of the node");
                    }
                } else {
                //CONSTRAINT: If a node n has a fold f then n's height should differ from f's height by at most 1
                    validationFailed = printErrorMessage(validationFailed, node,
                        "    This node has the fold " + fold.getWebId() + " with height " + fold.getHeight() + ".  They should only differ in height by at most 1");          
                }
            } else {
                //CONSTRAINT: A node n must have a fold iff
                //            1) There exists a node in the hypeerWeb that has the same height as n and their webIds are complements of each other OR
                //            2) There exists a node f with height one less than n's and f's webId padded to the left with one 0 is the complement of n's webId OR
                //            3) There exists a node f with height one more than n's and n's webId padded to the left with one 0 is the complement of f's webId
                if(expectedFold != null) {
                    validationFailed = printErrorMessage(validationFailed, node,
                        "    Node " + node.getWebId() + "(" + Long.toBinaryString(node.getWebId()) + ")'s  does not have a fold. It should have the fold " + expectedFold.getWebId());
                }
            }
        }
        return validationFailed;
    }
    
    /**
     * Check a node to see if all of the constraints on a possibly associated surrogate fold hold
     *          
     * @param node The node to be checked.
     * @param validationFailed Used to determine whether to print the error header for errors.
     * 
     * @pre node != null
     * @post result = true =>
     *             if |hypeerWeb| = 1 then its single node (node 0) must not have a surrogate fold AN
     *             if this node has a fold it may not have a surrogate fold AND
     *             if this node does not have a fold it must have
     *                 a surrogate fold, sf, whose height is one less than this node's and 
     *              the complement of this node's webId with its high-order bit removed = sf
     *      ELSE
     *          result = false
     */
    private boolean checkSurrogateFoldConstraints(NodeInterface node, boolean validationFailed) {
        boolean hypeerWebWithSingleNode = nodes.length == 1;
        
        if(hypeerWebWithSingleNode) {  //|hypeerWeb| = 1
            //CONSTRAINT: If the |hypeerWeb| = 1 then the single node in the hypeerweb (node 0) cannot have a surrogate fold
            if(node.getFold() != null) {
                validationFailed = printErrorMessage(validationFailed, node,
                    "    If the |hypeerWeb| == 1 then the single node in the hypeerWeb, node 0, should have no surogate fold but has " + node.getFold().getWebId() + " as a surrogate fold"); 
            }
        } else {
            NodeInterface fold = node.getFold();
            NodeInterface surrogateFold = node.getSurrogateFold();
            if(fold != null) {
                //CONSTRAINT: If a node n has a fold f it cannot have a surrogate fold
                if(surrogateFold != null) {
                    validationFailed = printErrorMessage(validationFailed, node,
                        "    If a node has a fold it cannot have surrogate fold but has the surrogate fold " + surrogateFold.getWebId() +
                        "(" + Long.toBinaryString(surrogateFold.getWebId()) + ")");
                } 
            } else {        
                long expectedSurrogateFold = getComplement(node, node.getHeight() -1);
                //CONSTRAINT: If a node n does not have a fold it must have a surrogate fold.
                if(surrogateFold == null) {
                    validationFailed = printErrorMessage(validationFailed, node,
                        "    This node has no fold and thus should have a surrogate fold " + expectedSurrogateFold + " but does not");
                //CONSTRAINT: If there exist a node sf such that its height is one less than n's and the complement of n's webId = sf's webId with a 0 padded to the left then
                //              sf is the surrogate fold of n.
                } else if(surrogateFold.getWebId() != expectedSurrogateFold) {
                    validationFailed = printErrorMessage(validationFailed, node,
                    "    This node has no fold and thus should have a surrogate fold " + expectedSurrogateFold + " but instead has the surrogate fold " + surrogateFold.getWebId());
                }
            }
        }
        return validationFailed;
    }
    
    /**
     * Check a node to see if all of the constraints on a possibly associated inverse surrogate fold hold
     * 
     * @param node The node to be checked.
     * @param validationFailed Used to determine whether to print the error header for errors.
     * 
     * @pre node != null
     * @post result = true =>    
     *             If |hypeerWeb| = 1 then this node (which is the node 0) must have not inverse surrogate fold AND
     *             If this node, n, has a fold, f, and n.height = f.height or n.height = f.height + 1 then n cannot have an inverse surrogate fold AND
     *             If this node, n, has a fold, f, and n.height + 1 = f.height then it must have an inverse surrogate fold, isf, such that the complement AND
     *                of n's webId padded with a leading 0 = isf AND
     *            If this node, n, has no fold it cannot have an inverse surrogate fold
     *         ELSE
     *             result = false
     */
    private boolean checkInverseSurrogateFoldConstraints(NodeInterface node, boolean validationFailed) {
        NodeInterface fold = node.getFold();
        NodeInterface inverseSurrogateFold = node.getInverseSurrogateFold();
        if(nodes.length == 1) { //the |hypeerWeb| = 1
            //CONSTRAINT: |hypeerWeb| = 1 => its single node (node 0) has no inverse surrogate node.
            if(inverseSurrogateFold != null) {
                validationFailed = printErrorMessage(validationFailed, node,
                    "    If the |hypeerWeb| == 1 then the single node in the hypeerWeb, node 0, should have no inverse surogate fold but has " + node.getFold().getWebId() + " as an inverse surrogate fold"); 
            }
        } else {
            if(node.getFold() != null) {
                //CONSTRAINT: If a node n has a fold f and they are of the same height it should not have an inverse surrogate fold.
                if(node.getHeight() == fold.getHeight()) {    
                    if(inverseSurrogateFold != null) {
                        validationFailed = printErrorMessage(validationFailed, node,
                            "    Since this node and its fold are of equal height it should not have the inverse surrogate fold " +
                            inverseSurrogateFold.getWebId() + "(" + Long.toBinaryString(inverseSurrogateFold.getWebId()) + ")");
                    }
                //CONSTRAINT: If a node n has a fold f and n.height = f.height + 1 it should not have an inverse surrogate fold
                } else if(node.getHeight() == fold.getHeight() + 1) {
                    if(inverseSurrogateFold != null) {
                        validationFailed = printErrorMessage(validationFailed, node,
                            "    Since the height of this node is one greater than the height of its fold " + fold.getWebId() +
                            "(height = " + fold.getHeight() + "), there should be no inverse surrogate fold but it has the inverse surrogate fold " +
                            inverseSurrogateFold.getWebId());
                    }
                //CONSTRAINT: If a node n has a fold f and n.height + 1 = f.height then it should have an inverse surrogate fold, isf, such that
                //            isf.height = n.height and the webIds (with n's webId padded with a leading 0) are complements of each other
                } else if(node.getHeight() + 1 == fold.getHeight()) {                
                    NodeInterface expectedInverseSurrogateFold = hypeerWeb.getNode(getComplement(node, node.getHeight()));
                    if(inverseSurrogateFold.getWebId() != expectedInverseSurrogateFold.getWebId()) {
                        validationFailed = printErrorMessage(validationFailed, node,
                            "    Since the height of this node is one less than the height of its fold " + fold.getWebId() +"(height = " + fold.getHeight() + ") it should have the inverse surrogate fold " +
                            expectedInverseSurrogateFold.getWebId() + " but does not");
                    }
                }
            } else {
                //CONSTRAINT: If a node n has no fold f then n must not have an inverse surrogate fold.
                if(inverseSurrogateFold != null) {
                    validationFailed = printErrorMessage(validationFailed, node,
                        "    This node has no fold and thus should have no inverse surrogate fold but it has the inverse surrogate fold " + inverseSurrogateFold.getWebId()); 
                }
            }
        }
        return validationFailed;
    }
    
    /**
     * Check a node to see if all of the height constraints its neighbors neighbor, neighbors neighbors surrogate neighbors, and neighbors neighbors fold hold.
     * These constraints are only checked if this is not a perfect hypercube of size 1
     * 
     * @param node The node to be checked.
     * @param validationFailed Used to determine whether to print the error header for errors.
     * 
     * @pre node != null
     * @post result = true =>
     *             this is a perfect hypercube of height 1 OR
     *                 (The difference between this node's height and any of it's neighbor's neighbor's height must <= 1 AND
     *                  The difference between this node's height and any of it's neighbor's neighbor's surrogate neighbors's height must > 0 AND <= 1 AND
     *                  The difference between this node's height and any of it's neighbor's neighbor's surrogate fold's height must > 0 AND <= 1
     *                 )
     *         ELSE
     *             result = false
     * 
     */
    private boolean checkNeighborsNeighborsConstraints(NodeInterface node, boolean validationFailed) {
        boolean perfectHyPeerCubeOfHeight1 = hypeerWeb.getNode(0).getHeight() == 1 && hypeerWeb.getNode(1).getHeight() == 1;
        if(!perfectHyPeerCubeOfHeight1) {  //In a perfect HyPeerWeb of height 1 there is no need to check a neighbors neighbors any thing because a node's neighbors neighbors is itself        
            NodeInterface[] neighbors = node.getNeighbors();
            HashSet<NodeInterface> nodesSeenSoFar = new HashSet<NodeInterface>();
            for(NodeInterface neighbor : neighbors) {
                if(!nodesSeenSoFar.contains(neighbor)) {
                    NodeInterface[] neighborsNeighbors = neighbor.getNeighbors();
                    for(NodeInterface neighborsNeighbor : neighborsNeighbors) {
                        if(neighborsNeighbor.getWebId() != node.getWebId()) {
                            if(!nodesSeenSoFar.contains(neighborsNeighbor)) {
                                boolean appropriateHeight = Math.abs(node.getHeight() - neighborsNeighbor.getHeight()) <= 1;
                                //CONSTRAINT: The difference between this node's height and it's neighbor's neighbor's height must <= 1
                                if(!appropriateHeight) {
                                    validationFailed = printErrorMessage(validationFailed, node,
                                        "    Expected the neighbor (" + neighbor.getWebId() + ")'s neighbor(" + neighborsNeighbor.getWebId() + "), with height " + neighborsNeighbor.getHeight() +
                                                       " to have the same height or be one less than " + node.getWebId() + "'s height " + node.getHeight());
                                }
                            
                                NodeInterface[] neighborsNeighborsSurrogateNeighbors = neighborsNeighbor.getSurrogateNeighbors();
                                for(NodeInterface neighborsNeighborsSurrogateNeighbor : neighborsNeighborsSurrogateNeighbors) {
                                    if(!nodesSeenSoFar.contains(neighborsNeighborsSurrogateNeighbor) && neighborsNeighborsSurrogateNeighbor.getWebId() != node.getWebId()) {
                                        //CONSTRAINT: The difference between this node's height and it's neighbor's neighbor's surrogate neighbors's height must > 0 AND <= 1
                                        if(node.getHeight() > neighborsNeighborsSurrogateNeighbor.getHeight() + 1 || node.getHeight() < neighborsNeighborsSurrogateNeighbor.getHeight()) {
                                            validationFailed = printErrorMessage(validationFailed, node,
                                                "    Expected the neighbor " + neighbor.getWebId() + "'s neighbor " + neighborsNeighbor.getWebId() + "'s surrogate neighbor, " + neighborsNeighborsSurrogateNeighbor.getWebId() +
                                                ", with height " + neighborsNeighborsSurrogateNeighbor.getHeight() + " to have the same height or be one less than " + node.getWebId() + "'s height " + node.getHeight());
                                        }
                                        nodesSeenSoFar.add(neighborsNeighborsSurrogateNeighbor);
                                    }
                                }
                                
            
                                NodeInterface neighborsNeighborsSurrogateFold = neighborsNeighbor.getSurrogateFold();
                                if(neighborsNeighborsSurrogateFold != null && !nodesSeenSoFar.contains(neighborsNeighborsSurrogateFold) && neighborsNeighborsSurrogateFold.getWebId() != node.getWebId()) {
                                    //CONSTRAINT: The difference between this node's height and it's neighbor's neighbor's surrogate fold's height must > 0 AND <= 1
                                    if(node.getHeight() > neighborsNeighborsSurrogateFold.getHeight() + 1 || node.getHeight() < neighborsNeighborsSurrogateFold.getHeight()) {
                                            validationFailed = printErrorMessage(validationFailed, node, 
                                                    "    Expected the neighbor " + neighbor.getWebId() + "'s neighbor " + neighborsNeighbor.getWebId() + "'s surrogate fold, " + neighborsNeighborsSurrogateFold.getWebId() +
                                                    ", with height " + neighborsNeighborsSurrogateFold.getHeight() + " to have the same height or be one less than " + node.getWebId() + "'s height " + node.getHeight());
                                    }
                                }
                                nodesSeenSoFar.add(neighborsNeighborsSurrogateFold);
                            }
                        }
                    }
                    nodesSeenSoFar.add(neighbor);
                }
            }
        }
        
        return validationFailed;
    }
    
    /**
     * Prints the given message as an error.
     * 
     * @param validationFailed Used to determine whether to print the error header for this node.
     * @param node The node we are printing the error message for
     * @param message  The message to be printed
     * 
     * @pre node != null AND message != null
     * @post result = true AND
     *       if validation has not failed print the header for this node's errors
     *       then print the given message.
     */
    private boolean printErrorMessage(boolean validationFailed, NodeInterface node, String message) {
        if(!validationFailed) {
            System.err.println("VALIDATION FAILED for node " + node.getWebId() + " with height " + node.getHeight());
        }
        System.err.println(message);
        return true;
    }    
    
    /**
     * Computes the distance between any two nodes.  The distance is the number of places in with the binary representation
     * of the webIds of these two nodes differ.  If the binary representation of either node is smaller than the other then
     * pad it with leading 0s until their lengths are the same and then determine the number of bits that are different.
     * 
     * FOR THE POST CONDITION:
     *         The notion of "number of places in which a binary representation b1 differs from a binary representation b2
     *         assumes that b1 and b2 are of the same length and is defined as 
     *             result = NumberOf i where 0 <= i < |b1| (b1[i] != b2[i])
     * 
     *         The function padLeft(BinaryRepresentation x, int y) -- assuming y is >= 0 is defined as
     *             result = zeros concatenated with x -- where zeros is string of zeros of length y
     * 
     * @param node1 The first node to compare
     * @param node2 The second node to compare
     * @pre node1 != null AND node2 != null
     * @post
     *         if |node1.webId| - |node2.webId| >= 0 then
     *             result = number of places in which node1.webId.binaryRepresentation differs from 
     *                 padLeft(node2.webId.binaryRepresentation, |node1.webId.binaryRepresentation| - |node2.webId.binaryRepresentation|)
     *         else
     *             result =  number of places in which padeLeft(node1.webId.binaryRepresentation, |node2.webId.binaryRepresenation| - |node1.webId.binaryRepresentation|)
     *                 differs from node2.webId.binaryRepresentation
     */
    private int distanceTo(NodeInterface node1, NodeInterface node2) {
        long xor = (node1.getWebId() ^ node2.getWebId());
        int result = 0;
        int mask = 1;
        for(int i = 0; i < NUMBER_OF_BITS_IN_A_LONG; i++) {
            long bit = xor & mask;
            if(bit != 0) {
                result++;
            }
            mask <<= 1;
        }
        return result;
    }
    
    /**
     * If there exists a one bit in the binary representation of node's webId then
     *         return the position counting from the right and starting with position 0
     *                where there is a 1 bit but everything to the left is 0's
     * else
     *         return -1
     * 
     * Example: if the webId = 0 (binary representation 0) return -1
     *             if the webId = 1 (binary representation 1) return 0
     *             if the webId = 2 (binary representation 10) return 1
     *          if the webId = 5 (binary representation 101) return 2
     * @param node The node that we are going to determine the position of the highest-order 1 bit in the binary representation of its webId
     * 
     * @pre node != null
     * @post 
     *         if there exists a one bit in the binary representation of node's webId =>
     *             result = the position counting from the right and starting with position 0
     *                where there is a 1 bit but everything to the left is 0's
     *         else
     *             result = -1
     */
    private int positionOfHighestOrderOneBit(NodeInterface node) {
        int result = -1;
        int loc = 0;
        long mask = 1l;
        long nodesWebId = node.getWebId();
        while(nodesWebId != 0) {
            if((nodesWebId & mask) != 0) {
                result = loc;
                nodesWebId = nodesWebId & ~mask;
            }
            mask <<= 1;
            loc++;
        }
        return result;
    }
    
    /**
     * Returns the fold of the given node if it has one, otherwise return null
     * 
     * @param node The node we are going to get the fold of
     * 
     * @pre node != null
     * @post
     *         if |hypeerWeb| = 1 then result null AND
     *         if |hypeerWeb| > 1 AND there does not exist a node whose webId = complement(node, node.height) then result = null
     *         if |hypeerWeb| > 1 AND there exists a node, nodesComplement, whose webId = getComplement(node, node.height) then
     *             node.height >= complement.height then result = complement
     *             node.height < commplement.height then result = the node in the hypeerWeb whose webId = getComplement(node, node.height + 1);    
     */
    private NodeInterface getFold(NodeInterface node) {
        NodeInterface result = null;
        if(node.getWebId() != 0 || node.getHeight() != 0) { //More than one node in HyPeerWeb
            NodeInterface complement = hypeerWeb.getNode(getComplement(node, node.getHeight()));
            if(complement != null) {
                if(complement.getHeight() == -1) {
                    result = null;
                } else if(node.getHeight() >= complement.getHeight()) {
                    result = complement;
                } else {
                    result = hypeerWeb.getNode(getComplement(node, node.getHeight() + 1));
                }
            }
        }
        return result;
    }

    /**
     * Compute the complement of node's webId where all but the last 'height' digits are set to 0
               setting all 
     * 
     * @param node The node whose webId we are going to take the complement of
     * @param height The number of digits in the complement of node's webId we are going to keep.
     *        The rest will be set to 0.
     * @pre node != null AND height >= 0
     * @post result = setAllButTheLastHeightDigitsTo0(complement(node.webId),height) 
     */
    public int getComplement(NodeInterface node, int height) {
        int mask = 0;
        if(node.getHeight() > 0) {
            mask = -1 >>> (32 - height);
        }
        int result = ~node.getWebId() & mask;
        return result;
    }
    
//Non-Public Constants
    private static final int NUMBER_OF_BITS_IN_A_LONG = 64;
}