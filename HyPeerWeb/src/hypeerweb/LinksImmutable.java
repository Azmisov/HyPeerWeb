package hypeerweb;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.TreeSet;

/**
 * A Links object that does not serialize to a Proxy
 * @author ljnutal6
 */
public class LinksImmutable implements Serializable{
	
	public final Node fold;
	public final Node surrogateFold;
	public final Node inverseSurrogateFold;
	public final TreeSet<Node> neighbors;
	public final TreeSet<Node> surrogateNeighbors;
	public final TreeSet<Node> inverseSurrogateNeighbors;
	public final TreeSet<Node> highest;
	
	public LinksImmutable(Links links){
		fold = links.fold;
		surrogateFold = links.surrogateFold;
		inverseSurrogateFold = links.inverseSurrogateFold;
		neighbors = links.neighbors;
		surrogateNeighbors = links.surrogateNeighbors;
		inverseSurrogateNeighbors = links.inverseSurrogateNeighbors;
		highest = links.highest;
	}
}
