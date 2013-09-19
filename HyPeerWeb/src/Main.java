
/**
 * Dummy class for testing out your code
 */
public class Main {
	public static void main(String[] args){
		try {
			//Make sure database is working
			Database db = Database.getInstance();
		} catch (Exception ex) {
			System.out.println("DB creation failed");
		}
	}
}
