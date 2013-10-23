package hypeerweb;

import hypeerweb.Node;
import java.util.ArrayList;
import java.util.HashSet;

//VISITOR PATTERN
	public interface VisitorInterface{
		public void visit(Node n);
	
	public class VisitorSend implements VisitorInterface{
		int target;

		public VisitorSend(int t) {
			target = t;
		}

		@Override
		public void visit(Node n) {
			if (n.getWebId() == target) {
				return; //To be replaced with the action to be performed
			}
			int distance = distance(n.getHeight(), n.getWebId());
			ArrayList<Node> candidates = new ArrayList<Node>();
			for (Node nn : n.getNeighbors()) {
				if (distance(nn.getHeight(), nn.getWebId()) > distance) {
					candidates.add(nn);
				}
			}
			for (Node nn : n.getSurrogateNeighbors()) {
				if (distance(nn.getHeight(), nn.getWebId()) > distance) {
					candidates.add(nn);
				}
			}
			for (Node nn : n.getInverseSurrogateNeighbors()) {
				if (distance(nn.getHeight(), nn.getWebId()) > distance) {
					candidates.add(nn);
				}
			}
			Node node = n.getFold();
			if (node != null) {
				if (distance(node.getHeight(), node.getWebId()) > distance) {
					candidates.add(node);
				}
			}
			node = n.getSurrogateFold();
			if (node != null) {
				if (distance(node.getHeight(), node.getWebId()) > distance) {
					candidates.add(node);
				}
			}
			node = n.getInverseSurrogateFold();
			if (node != null) {
				if (distance(node.getHeight(), node.getWebId()) > distance) {
					candidates.add(node);
				}
			}
			if (candidates.isEmpty()) {
				for (Node nn : n.getInverseSurrogateNeighbors()) {
					if (distance(nn.getHeight(), nn.getWebId()) >= distance) {
						candidates.add(nn);
					}
				}
			}
			if (candidates.isEmpty()) {
				System.err.println("Unexpected: Send can't get any closer to node " + target
						+ "than node " + n.getWebId());
			} else {
				visit(candidates.get(0));
			}
			//Do something here...
		}

		private int distance(int ht, int ID) {
			int dist = 0;
			int mask = 1;
			for (int i = 0; i < ht; i++) {
				if ((mask & ID) == (mask & target)) {
					dist++;
				}
				mask *= 2;
			}
			return dist;
		}
	private class VisitorBroadcast implements VisitorInterface{
            @Override
            public void visit(Node n){
				int webID = n.getWebId();
				
                int trailingZeros = Integer.numberOfTrailingZeros(webID);
                HashSet<Integer> generatedNeighbors = new HashSet();
                HashSet<Integer> generatedInverseSurrogates = new HashSet();
                int neighbors = webID;
                int inverseSurrogates = webID | (Integer.highestOneBit(webID) << 1);
                int bitShifter = 1;
                for(int i = 0; i < trailingZeros; i++){
                    generatedNeighbors.add(neighbors | bitShifter);
                    generatedInverseSurrogates.add(inverseSurrogates | bitShifter);
                    bitShifter <<= 1;
                }
                for(Node node : n.L.getNeighborsSet()){
                    if(generatedNeighbors.contains(node.getWebId())){
                        //do something
                    }
                }
                for(Node node : n.L.getInverseSurrogateNeighborsSet()){
                    if(generatedInverseSurrogates.contains(node.getWebId())){
                        //do something
                    }
                }
            }
        }
	}
}