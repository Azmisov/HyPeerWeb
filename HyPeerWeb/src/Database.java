
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Low-level Database access class
 * @author isaac
 * TODO, future:
 * - make prepared statement interface
 * - update removeNode test cases
 * - disable auto-commit for mass edits (addNode)
 * - reuse auto-commit code
 */
public class Database {
	//Reference to singleton
	private static Database singleton;
	private static boolean IS_CONNECTED = true;
	//Database connection and default statement
	private Connection db;
	private Statement stmt;
	//Databse commit management
	private StringBuilder sqlbuffer;
	private boolean autocommit = true,
					commitFail = false;
	private int commitStack = 0;

	/**
	 * Private constructor for the singleton - Initializes database connection -
	 * Creates database if it doesn't exist
	 *
	 * @author isaac
	 */
	private Database() throws Exception {
		//Load the database driver
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			System.out.println("Could not load database driver!");
			throw e;
		}
		//Open a database connection
		try {
			db = DriverManager.getConnection("jdbc:sqlite:HyPeerWeb.sqlite");
		} catch (Exception e) {
			System.out.println("Could not connect to the database!");
			throw e;
		}
		//Setup the database, if not already there
		try {
			String[] db_setup = {
				"BEGIN;", 
				"create table if not exists `Nodes` (`WebId` integer primary key, `Height` integer, `Fold` integer, `SurrogateFold` integer, `InverseSurrogateFold` integer);", 
				"create table if not exists `Neighbors` (`WebId` integer, `Neighbor` integer);", 
				"create table if not exists `SurrogateNeighbors` (`WebId` integer, `SurrogateNeighbor` integer);", 
				"create index if not exists `Idx_Neighbors` on `Neighbors` (`WebId`);", 
				"create index if not exists `Idx_InverseNeighbors` on `Neighbors` (`Neighbor`);", 
				"create index if not exists `Idx_SurrogateNeighbors` on `SurrogateNeighbors` (`WebId`);", 
				"create index if not exists `Idx_InverseSurrogateNeighbors` on `SurrogateNeighbors` (`SurrogateNeighbor`);",
				"COMMIT;"
			};
			stmt = db.createStatement();
			stmt.setQueryTimeout(5);
			for (int i = 0; i < db_setup.length; i++) {
				stmt.executeUpdate(db_setup[i]);
			}
		} catch (SQLException e) {
			System.out.println("Could not create the database!");
			throw e;
		}
		sqlbuffer = new StringBuilder();
	}
	/**
	 * Gets the singleton instance of the Database class
	 *
	 * @return singleton Database reference
	 * @author isaac
	 */
	public static Database getInstance() throws Exception {
		//First time connect
		if (IS_CONNECTED && singleton == null) {
			try {
				singleton = new Database();
			} catch (Exception e) {
				IS_CONNECTED = false;
				throw e;
			}
		}
		//Failed connection
		if (!IS_CONNECTED) {
			throw new Exception("Failed to connect to database");
		}
		//Successful connection available
		return singleton;
	}
	/**
	 * Removes all data from the database, leaving the structure intact.
	 * @author john
	 */
	public void clear() {
		sqlUpdate("delete from `Nodes`");
		sqlUpdate("delete from `Neighbors`");
		sqlUpdate("delete from `SurrogateNeighbors`");
	}

	///SQL OPERATIONS
	/**
	 * Run an SQL update statement
	 *
	 * @param sql the sql statement
	 * @return true if the operation was successful
	 * @author isaac
	 */
	private boolean sqlUpdate(String sql) {
		try {
			if (autocommit)
				stmt.executeUpdate(sql);
			else if (!commitFail){
				sqlbuffer.append(sql);
				if (sql.charAt(sql.length()-1) != ';')
					sqlbuffer.append(';');
				if (sql.equals("COMMIT;"))
					stmt.executeUpdate(sqlbuffer.toString());
			}
		} catch (Exception e) {
			System.out.println("SQL Error: " + e.getMessage());
			return false;
		}
		return true;
	}
	/**
	 * Run an SQL query statement
	 *
	 * @param sql the sql query
	 * @return the ResultSet of the query, if successful
	 * @throws Exception the error, if the operation failed
	 * @author isaac
	 */
	private ResultSet sqlQuery(String sql) throws Exception {
		return stmt.executeQuery(sql);
	}
	
	//COMMIT MANAGEMENT
	/**
	 * Starts a commit
	 * @author isaac
	 */
	public void beginCommit(){
		if (commitStack == 0){
			commitFail = false;
			autocommit = false;
			sqlbuffer.append("BEGIN;");
		}
		commitStack++;
	}
	/**
	 * Ends a commit
	 * @return true, if the commit was successful
	 * @author isaac
	 */
	public boolean endCommit(){
		if (--commitStack == 0){
			boolean ret = commitFail ? false : sqlUpdate("COMMIT;");
			clearCommit();
			return ret;
		}
		return true;
	}
	/**
	 * Clears a commit, should an exception occur throughout the process
	 * @author isaac
	 */
	private void clearCommit(){
		if (!commitFail){
			sqlbuffer = new StringBuilder();
			autocommit = true;
		}
		if (--commitStack != 0)
			commitFail = true;
	}
	
	///FULL NODE OPERATIONS
	/**
	 * Add a node to the database
	 *
	 * @param node the node to add
	 * @return true if the node was successfully added
	 * @author guy
	 */
	public boolean addNode(Node node) {
		try {
			beginCommit();
			
                        int webid = node.getWebId();
                        
                        String update;
			update = "INSERT INTO Nodes (WebID) VALUES(" + webid + ");";
                        sqlUpdate(update);
			
                        setHeight(webid, node.getHeight());
                        if(node.getFold() != null)
                            setFold(webid, node.getFold().getWebId());
                        if(node.getSurrogateFold() != null)
                            setSurrogateFold(webid, node.getSurrogateFold().getWebId());
			if(node.getInverseSurrogateFold() != null)
                            setSurrogateFold(node.getInverseSurrogateFold().getWebId(), webid);

			Node[] list;

			list = node.getNeighbors();
			for (Node n : list)
				addNeighbor(webid, n.getWebId());
			
			//surrogate and inverse are reflexive
                        //if every node adds its SNs, the ISNs will also be recorded
			list = node.getSurrogateNeighbors();
			for (Node n : list)
				addSurrogateNeighbor(webid, n.getWebId());
			
			return endCommit();
		} catch (Exception e) {
			clearCommit();
			return false;
		}
	}
	/**
	 * Add a node to the database
	 *
	 * @param webid the node's WebID
	 * @param height the node's height
	 * @param fold the WebID of the node's Fold
	 * @param sfold the WebID of the surrogate
	 * @return true if the node was successfully added
	 * @author brian
	 */
	public boolean addNode(int webid, int height, Node fold, Node sfold, Node isfold) {
		return addNode(new Node(webid, height, fold, sfold, isfold,
                    null, null, null));
	}
	/**
	 * Removes a node from the database
	 *
	 * @param webid the WebID of the node to remove
	 * @return true if the node was successfully removed
	 * @author brian
	 */
	public boolean removeNode(int webid){
		beginCommit();
		sqlUpdate("DELETE FROM Nodes WHERE WebId=" + webid + ";");
		sqlUpdate("DELETE FROM Neighbors WHERE WebID=" + webid + " OR Neighbor="+ webid +";");
		sqlUpdate("DELETE FROM SurrogateNeighbors WHERE WebID=" + webid + ";");
		return endCommit();
	}
        
	/**
	 * Makes nodes from data stored in database
	 * @return a TreeSet<Node> containing all nodes stored in database
	 */
	public TreeSet<Node> getAllNodes() throws Exception{
		TreeSet<Node> tsnodes = new TreeSet();
		ArrayList<Node> nodes = new ArrayList();
		ResultSet rs;
		String sql;
		Node n;
		int i;

		//get data from Nodes table
		sql = "SELECT * FROM Nodes";
		rs = sqlQuery(sql);
		while(rs.next()){
			n = new Node(rs.getInt("WebId"), rs.getInt("Height"));
			nodes.add(n);
		}
		rs.beforeFirst();
		i = 0;
		while(rs.next()){
			n = nodes.get(i);
			if(rs.getInt("Fold") != -1)//need to set default value of f, sf, and isf to -1 because getInt will return 0 if entry in null
			n.setFold(n);
		}

		//get data from Neighbors table

		//get data from SurrogateNeighbors table

		//fill treeset from arraylist
		return tsnodes;
	}
        
        ///NODE ATTRIBUTES
	/**
	 * Set a node's height
	 *
	 * @param webid the WebId of the node to modify
	 * @param height the node's height
	 * @return true if the operation was successful
	 * @author isaac
	 */
	public boolean setHeight(int webid, int height) {
		return setColumn(webid, "Height", height);
	}
	/**
	 * Set the Fold node of another node
	 *
	 * @param webid the WebId of the node to modify
	 * @param foldid the WebId of the node's Fold
	 * @return true if the operation was successful
	 * @author isaac
	 */
	public boolean setFold(int webid, int foldid) {
		return setColumn(webid, "Fold", foldid);
	}
	/**
	 * Set the Surrogate Fold node of another node
	 * 
	 * @param webid the WebId of the node to modify
	 * @param sfoldid the WebId of the node's surrogate fold
	 * @return true if the operation was successful
	 * @author isaac
	 */
	public boolean setSurrogateFold(int webid, int sfoldid) {
		return setColumn(webid, "SurrogateFold", sfoldid);
	}
	/**
	 * Set the Inverse Surrogate Fold node of another node
	 * 
	 * @param webid the WebId of the node to modify
	 * @param sfoldid the WebId of the node's inverse surrogate fold
	 * @return true if the operation was successful
	 * @author isaac
	 */
	public boolean setInverseSurrogateFold(int webid, int isfoldid){
		return setColumn(webid, "InverseSurrogateFold", isfoldid);
	}
	/**
	 * Get a node's height
	 *
	 * @param webid the WebId of the node to access
	 * @return the node's height
	 * @throws Exception throws exception if retrieval fails
	 * @author isaac
	 */
	public int getHeight(int webid) throws Exception {
		return getColumn(webid, "Height");
	}
	/**
	 * Get the Fold node of another node
	 * @param webid the WebId of the node to access
	 * @return the WebId of the node's Fold
	 * @throws Exception throws exception if retrieval fails
	 * @author isaac
	 */
	public int getFold(int webid) throws Exception {
		return getColumn(webid, "Fold");
	}
	/**
	 * Get the Surrogate Fold node of another node
	 * @param webid the WebId of the node to access
	 * @return the WebId of the node's Surrogate Fold
	 * @throws Exception throws exception if retrieval fails
	 * @author isaac
	 */
	public int getSurrogateFold(int webid) throws Exception {
		return getColumn(webid, "SurrogateFold");
	}
	/**
	 * Gets the Inverse Surrogate Fold of another node
	 * @param webid the WebId of the node to access
	 * @return the WebId of the node's Inverse Surrogate Fold
	 * @throws Exception throws exception if retrieval fails
	 * @author isaac
	 */
	public int getInverseSurrogateFold(int webid) throws Exception {
		return getColumn(webid, "InverseSurrogateFold");
	}
	
	//private methods for getting/setting columns
	private boolean setColumn(int webid, String colname, int value) {
		return sqlUpdate("update `Nodes` set `" + colname + "` = '" + value + "' where `WebId` = '" + webid + "';");
	}
	private int getColumn(int webid, String colname) throws Exception {
		ResultSet res = sqlQuery("select `" + colname + "` as col_value from `Nodes` where `WebId` = '" + webid + "';");
		return res.getInt("col_value");
	}

	///NODE NEIGHBORS
	/**
	 * Add neighbor node to a node
	 *
	 * @param webid the WebId of the parent node
	 * @param neighbor WebId of the new neighbor
	 * @return true if neighbor was successfully added
	 * @author josh
	 */
	public boolean addNeighbor(int webid, int neighbor) {
		beginCommit();
		sqlUpdate("INSERT INTO Neighbors (WebId, Neighbor) VALUES ("+webid+", "+neighbor+");");
		sqlUpdate("INSERT INTO Neighbors (WebId, Neighbor) VALUES ("+neighbor+", "+webid+");");
		return endCommit();
	}
	/**
	 * Removes neighbor node from a node
	 *
	 * @param webid the WebId of the parent node
	 * @param neighbor WebId of the neighbor to remove
	 * @return true if neighbor was successfully removed
	 * @author josh
	 */
	public boolean removeNeighbor(int webid, int neighbor) {
		return sqlUpdate("DELETE FROM Neighbors WHERE "+
							"(WebId="+webid+" AND Neighbor = "+neighbor+") OR "+
							"(WebID="+neighbor+" AND Neighbor="+webid+");");
	}
	/**
	 * Retrieves a list of a node's neighbors
	 *
	 * @param webid the WebId of the node to access
	 * @return list of WebId's of the node's neighbors
	 * @throws Exception throws exception if there was an error in retrieval
	 * @author josh
	 */
	public List<Integer> getNeighbors(int webid) throws Exception {
		ResultSet set = sqlQuery("SELECT Neighbor FROM Neighbors WHERE WebId = "+webid+";");
		List<Integer> neighbors = new ArrayList<>();
		while (set.next())
			neighbors.add(set.getInt(1));
		return neighbors;
	}
	/**
	 * Add a surrogate neighbor to a node
	 *
	 * @param webid the WebId of the parent node
	 * @param neighbor the WebId of the neighbor node to add
	 * @return true, if the neighbor was successfully added
	 * @author john
	 */
	public boolean addSurrogateNeighbor(int webid, int neighbor) {
		return sqlUpdate("INSERT INTO SurrogateNeighbors VALUES(" + webid + "," + neighbor + ");");
	}
	/**
	 * Remove a surrogate neighbor from a node
	 *
	 * @param webid the WebId of the parent node
	 * @param neighbor the WebId of the neighbor node to remove
	 * @return true, if the neighbor was successfully removed
	 * @author john
	 */
	public boolean removeSurrogateNeighbor(int webid, int neighbor) {
		return sqlUpdate("DELETE FROM SurrogateNeighbors WHERE WebID=" + webid
				+ " AND SurrogateNeighbor=" + neighbor + ";");
	}
	/**
	 * Retrieves a list of surrogate neighbors
	 *
	 * @param webid the WebId of the original node
	 * @return a list of WebId's of the node's
	 * @throws Exception throws exception if there was an error in retrieval
	 * @author john
	 */
	public List<Integer> getSurrogateNeighbors(int webid) throws Exception {
		List<Integer> data = new ArrayList<>();
		ResultSet results = sqlQuery("select `SurrogateNeighbor` as `sn` from `SurrogateNeighbors` where `WebID` = '" + webid + "';");
		while (results.next())
			data.add(results.getInt("sn"));
		return data;
	}
	/**
	 * Retrieves a list of inverse surrogate neighbors
	 *
	 * @param webid the WebId of the surrogate neighbor
	 * @return a list of WebId's that are the surrogate neighbor's inverse
	 * @throws Exception throws exception if there was an error in retrieval
	 * @author john
	 */
	public List<Integer> getInverseSurrogateNeighbors(int webid) throws Exception {
		ArrayList<Integer> data = new ArrayList<>();
		ResultSet results = sqlQuery("select `WebId` as `webid` from `SurrogateNeighbors` where `SurrogateNeighbor` = " + webid + ";");
		while (results.next())
			data.add(results.getInt("webid"));
		return data;
	}
}
