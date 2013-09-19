import java.util.*;

/**
 * Dummy class for testing out your code
 */
public class Main {
	public static void main(String[] args){
		try {
			//Make sure database is working
			Database db = Database.getInstance();
                        testSurrogates(db);
		} catch (Exception ex) {
			System.out.println("DB creation failed");
		}
	}
        
        private static void testSurrogates(Database db)
        {
            try{
                db.clearDB();
                if (!db.getSurrogateNeighbors(1).isEmpty())
                    System.out.println("Found Surrogate Neighbors when database "
                            + "was supposed to be empty");
                if (!db.getInverseSurrogateNeighbors(1).isEmpty())
                    System.out.println("Found Inverse Surrogate Neighbors when "
                            + "database was supposed to be empty");
                
                db.addSurrogateNeighbor(4, 1);
                db.addSurrogateNeighbor(4, 2);
                db.addSurrogateNeighbor(7, 1);
                ArrayList<Integer> SurrList = db.getInverseSurrogateNeighbors(1);
                if (SurrList.size() != 2)
                    System.out.println("Expected Surrogate Neighbors: 2"
                            + "\nFound Surrogate Neighbors: " + SurrList.size());
                SurrList = db.getSurrogateNeighbors(4);
                if (SurrList.size() != 2)
                    System.out.println("Expected Surrogate Neighbors: 2"
                            + "\nFound Surrogate Neighbors: " + SurrList.size());
                db.removeSurrogateNeighbor(7, 1);
                if (SurrList.size() != 2)
                    System.out.println("Expected Surrogate Neighbors: 1"
                            + "\nFound Surrogate Neighbors: " + SurrList.size());
                System.out.println("Surrogate Neighbor Tests completed");
            }
            catch (Exception e) {
                System.out.println("Exception encountered");
            }
        }
}
