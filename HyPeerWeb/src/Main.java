import java.util.*;

/**
 * Dummy class for testing out your code
 */
public class Main {
	public static void main(String[] args){
		int total_errs = 0;
		try {
			System.out.println("BEGIN DATABASE CLASS TESTING:");
			//Make sure database is working
			Database db = Database.getInstance();
			total_errs += testSurrogates(db);
			int addNodeTestErrors = 0;
			//Add node test code must come before testNodeAttributes
			if (addNodeTestErrors == 0)
				total_errs += testNodeAttributes(db);
		} catch (Exception ex) {
			System.out.println("DB creation failed:\t"+ex.getMessage());
		}
	}
        
	private static int testSurrogates(Database db){
		int errs = 0;
		try{
			db.clearDB();
			if (!db.getSurrogateNeighbors(1).isEmpty()){
				System.out.println("Found Surrogate Neighbors when database "
						+ "was supposed to be empty");
				errs++;
			}
			if (!db.getInverseSurrogateNeighbors(1).isEmpty()){
				System.out.println("Found Inverse Surrogate Neighbors when "
						+ "database was supposed to be empty");
				errs++;
			}

			db.addSurrogateNeighbor(4, 1);
			db.addSurrogateNeighbor(4, 2);
			db.addSurrogateNeighbor(7, 1);
			ArrayList<Integer> SurrList = db.getInverseSurrogateNeighbors(1);
			if (SurrList.size() != 2){
				System.out.println("Expected Surrogate Neighbors: 2"
						+ "\nFound Surrogate Neighbors: " + SurrList.size());
				errs++;
			}
			SurrList = db.getSurrogateNeighbors(4);
			if (SurrList.size() != 2){
				System.out.println("Expected Surrogate Neighbors: 2"
						+ "\nFound Surrogate Neighbors: " + SurrList.size());
				errs++;
			}
			db.removeSurrogateNeighbor(7, 1);
			if (SurrList.size() != 2){
				System.out.println("Expected Surrogate Neighbors: 1"
						+ "\nFound Surrogate Neighbors: " + SurrList.size());
				errs++;
			}
			System.out.println("Surrogate Neighbor Tests completed");
		}
		catch (Exception e) {
			System.out.println("Exception encountered");
			errs++;
		}
		return errs;
	}
	
	private static int testNodeAttributes(Database db){
		System.out.println("---- BEGIN testNodeAttibutes ----");
		int errs = 0, temp;
		try{
			db.clearDB();
			db.addNode(new Node(5));
			//Height
			if (!db.setHeight(5, 10)) errs++;
			if (db.getHeight(5) != 10){
				System.out.println("Failed to set node height");
				errs++;
			}
			//Fold
			db.addNode(new Node(6));
			if (!db.setFold(5, 6)) errs++;
			if (db.getFold(5) != 6){
				System.out.println("Failed to set node fold");
				errs++;
			}
			//Surrogate fold
			if (!db.setSurrogateFold(5, 6)) errs++;
			if (db.getSurrogateFold(5) != 6){
				System.out.println("Failed to set surrogate fold");
				errs++;
			}
			if (db.getInverseSurrogateFold(6) != 5){
				System.out.println("Failed to set inverse surrogate fold");
				errs++;
			}
		} catch (Exception e){
			System.out.println("!! getColumn exception encountered, could not complete tests !!");
			errs++;
		}
		System.out.println("# errors = "+errs);
		System.out.println("---- END testNodeAttibutes ----");
		return errs;
	}
}
