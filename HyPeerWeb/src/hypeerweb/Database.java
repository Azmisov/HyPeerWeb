package hypeerweb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Low-level Database access class
 * @author isaac
 */
public final class Database {
	//Reference to databases we're managing
	private static HashMap<String, Database> singleton = new HashMap();
	//Has the database driver been loaded?
	private static boolean IS_CONNECTED = true;
	private static final String
			driverErr = "Could not load database driver!",
			connErr = "Could not connect to the database!",
			createErr = "Could not create the database!";
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
	 * @author isaac
	 */
	private Database(String dbName) throws Exception {
		//Load the database driver
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			System.out.println(driverErr);
			IS_CONNECTED = false;
			throw e;
		}
		//Open a database connection
		try {
			db = DriverManager.getConnection("jdbc:sqlite:"+dbName);
		} catch (SQLException e) {
			System.out.println(connErr);
			throw e;
		}
		//Setup the database, if not already there
		try {
			//This needs to be a single string, so we can execute in one batch command
			String db_setup =
				"BEGIN;"+
				"create table if not exists `Nodes` (`WebId` integer primary key, `Height` integer, `Fold` integer default -1, `SurrogateFold` integer default -1, `InverseSurrogateFold` integer default -1);"+
				"create table if not exists `Neighbors` (`WebId` integer, `Neighbor` integer);"+
				"create table if not exists `SurrogateNeighbors` (`WebId` integer, `SurrogateNeighbor` integer);"+
				"create index if not exists `Idx_Neighbors` on `Neighbors` (`WebId`);"+
				"create index if not exists `Idx_InverseNeighbors` on `Neighbors` (`Neighbor`);"+
				"create index if not exists `Idx_SurrogateNeighbors` on `SurrogateNeighbors` (`WebId`);"+
				"create index if not exists `Idx_InverseSurrogateNeighbors` on `SurrogateNeighbors` (`SurrogateNeighbor`);"+
				"COMMIT;";
			//This statement is used by sqlUpdate/sqlQuery
			//it should never be called directly
			stmt = db.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			stmt.setQueryTimeout(5);
			//(... except here) Setup the database
			stmt.executeUpdate(db_setup);
		} catch (SQLException e) {
			System.out.println(createErr);
			throw e;
		}
		sqlbuffer = new StringBuilder();
	}
	/**
	 * Gets the singleton instance of the Database class
	 * @param dbName the database name (e.g. HyPeerWeb.sqlite)
	 * @return singleton Database reference
	 * @throws Exception if there was an error connecting to the database
	 * @author isaac
	 */
	public static Database getInstance(String dbName) throws Exception {
		//Failed to load DB driver
		if (!IS_CONNECTED)
			throw new Exception(driverErr);
		Database instance = null;
		//Previously created database
		if (singleton.containsKey(dbName)){
			instance = singleton.get(dbName);
			if (instance == null)
				throw new Exception(connErr);
			return instance;
		}
		//First time connect
		try {
			instance = new Database(dbName);
		} catch (Exception e) {
			throw e;
		} finally{
			if (IS_CONNECTED)
				singleton.put(dbName, instance);
		}
		//Successful connection available
		return instance;
	}
	/**
	 * Removes all data from the database, leaving the structure intact.
	 * @return true if we could successfully clear the database
	 * @author john
	 */
	protected boolean clear(){
		beginCommit();
		sqlUpdate("delete from `Nodes`;");
		sqlUpdate("delete from `Neighbors`;");
		sqlUpdate("delete from `SurrogateNeighbors`;");
		return endCommit();
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
	protected void beginCommit(){
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
	protected boolean endCommit(){
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
	 * Replace a node
	 * @param removeWebID the old webid
	 * @param replaceWebID the new one
	 * @param replacement the replacement 
	 * @return true, if the operation was successful; if there is already
	 * a row with new_webid, it will fail
	 */
	protected boolean replaceNode(int removeWebID, int replaceWebID, Node replacement){
		beginCommit();
		removeNode(removeWebID);
		removeNode(replaceWebID);
		addNode(replacement);
		return endCommit();
	}
	/**
	 * Add a node to the database
	 *
	 * @param node the node to add
	 * @return true if the node was successfully added
	 * @author guy
	 */
	protected boolean addNode(Node node) {
		beginCommit();

		//Nodes table
		Node tempFold = node.getFold(),
			 tempSFold = node.getSurrogateFold(),
			 tempISFold = node.getInverseSurrogateFold();
		sqlUpdate("INSERT INTO `Nodes` VALUES ("+
					node.getWebId() + "," +
					node.getHeight() + "," +
					(tempFold == null ? -1 : tempFold.getWebId()) + "," +
					(tempSFold == null ? -1 : tempSFold.getWebId()) + "," +
					(tempISFold == null ? -1 : tempISFold.getWebId()) + ");");

		//Neighbors table
		Node[] temp = node.getNeighbors();
		for (Node n : temp)
			addNeighbor(node.getWebId(), n.getWebId());

		//Surrogates table (Inverse is reflexive)
		for (Node n: node.getSurrogateNeighbors())
			addSurrogateNeighbor(node.getWebId(), n.getWebId());

		return endCommit();
	}
	/**
	 * Add a node to the database
	 *
	 * @param webid the node's WebID
	 * @param height the node's height
	 * @param fold the WebID of the node's fold
	 * @param sfold the WebID of the node's surrogate fold
	 * @param isfold the WebID of the node's inverse surrogate fold
	 * @return true if the node was successfully added
	 * @author brian
	 */
	protected boolean addNode(int webid, int height, Node fold, Node sfold, Node isfold) {
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
	protected boolean removeNode(int webid){
		beginCommit();
		sqlUpdate("DELETE FROM Nodes WHERE WebId=" + webid + ";");
		sqlUpdate("DELETE FROM Neighbors WHERE WebID=" + webid + " OR Neighbor="+ webid +";");
		sqlUpdate("DELETE FROM SurrogateNeighbors WHERE WebID=" + webid + ";");
		return endCommit();
	}
	
	/**
	 * Makes nodes from data stored in database
	 * @return a TreeSet<Node> containing all nodes stored in database
	 * @throws Exception if retrieval fails
	 */
	public TreeMap<Integer, Node> getAllNodes() throws Exception{
		TreeMap<Integer, Node> tsnodes = new TreeMap<>();
		ResultSet rs;
		Node n, left, right, raw;
		int id;

		//Get data from Nodes table
		rs = sqlQuery(
				"SELECT `WebID`,`Height`,`Fold`,`SurrogateFold`,`InverseSurrogateFold`"
				+ "FROM `Nodes` ORDER BY `WebID` ASC");
		//Create raw nodes (no neighbor links)
		while (rs.next()){
			id = rs.getInt("WebId");
			raw = tsnodes.get(id);
			//Create a new node
			if (raw == null){
				raw = new Node(id, rs.getInt("Height"));
				tsnodes.put(id, raw);
			}
			//Update a temporary node
			else raw.setHeight(rs.getInt("Height"));
			//Initialize folds
			if ((id = rs.getInt("Fold")) != -1){
				n = tsnodes.get(id);
				if (n == null){
					n = new Node(id, 0);
					tsnodes.put(id, n);
				}
				raw.L.setFold(n);
			}
			if ((id = rs.getInt("SurrogateFold")) != -1){
				n = tsnodes.get(id);
				if (n == null){
					n = new Node(id, 0);
					tsnodes.put(id, n);
				}
				raw.L.setSurrogateFold(n);
			}
			if ((id = rs.getInt("InverseSurrogateFold")) != -1){
				n = tsnodes.get(id);
				if (n == null){
					n = new Node(id, 0);
					tsnodes.put(id, n);
				}
				raw.L.setInverseSurrogateFold(n);
			}
		}
		if (tsnodes.isEmpty())
			return tsnodes;

		//get data from Neighbors table
		rs = sqlQuery("SELECT `WebID`,`Neighbor` FROM `Neighbors`");
		while(rs.next()){
			left = tsnodes.get(rs.getInt("WebId"));
			right = tsnodes.get(rs.getInt("Neighbor"));
			left.L.addNeighbor(right);
		}
				
		//get data from SurrogateNeighbors table
		rs = sqlQuery("SELECT `WebId`,`SurrogateNeighbor` FROM SurrogateNeighbors");
		while(rs.next()){
			left = tsnodes.get(rs.getInt("WebId"));
			right = tsnodes.get(rs.getInt("SurrogateNeighbor"));
			left.L.addSurrogateNeighbor(right);
			right.L.addInverseSurrogateNeighbor(left);
		}
		
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
	protected boolean setHeight(int webid, int height) {
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
	protected boolean setFold(int webid, int foldid) {
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
	protected boolean setSurrogateFold(int webid, int sfoldid) {
		return setColumn(webid, "SurrogateFold", sfoldid);
	}
	/**
	 * Set the Inverse Surrogate Fold node of another node
	 * @param webid the WebId of the node to modify
	 * @param isfoldid the WebId of the node's surrogate fold
	 * @return true if the operation was successful
	 * @author isaac
	 */
	protected boolean setInverseSurrogateFold(int webid, int isfoldid) {
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
	 * @return the WebId of the node's Fold or -1 if the node does not have a fold
	 * @throws Exception throws exception if retrieval fails
	 * @author isaac
	 */
	public int getFold(int webid) throws Exception {
		return getColumn(webid, "Fold");
	}
	/**
	 * Get the Surrogate Fold node of another node
	 * @param webid the WebId of the node to access
	 * @return the WebId of the node's Surrogate Fold or -1 if the node does not have a surrogate fold
	 * @throws Exception throws exception if retrieval fails
	 * @author isaac
	 */
	public int getSurrogateFold(int webid) throws Exception {
		return getColumn(webid, "SurrogateFold");
	}
	/**
	 * Gets the Inverse Surrogate Fold of another node
	 * @param webid the WebId of the node to access
	 * @return the WebId of the node's Inverse Surrogate Fold or -1 if the node does not have a inverse surrogate fold
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
	protected boolean addNeighbor(int webid, int neighbor) {
		return sqlUpdate("INSERT INTO Neighbors (WebId, Neighbor) VALUES ("+webid+", "+neighbor+");");
	}
	/**
	 * Removes neighbor node from a node
	 *
	 * @param webid the WebId of the parent node
	 * @param neighbor WebId of the neighbor to remove
	 * @return true if neighbor was successfully removed
	 * @author josh
	 */
	protected boolean removeNeighbor(int webid, int neighbor) {
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
	protected boolean addSurrogateNeighbor(int webid, int neighbor) {
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
	protected boolean removeSurrogateNeighbor(int webid, int neighbor) {
		return sqlUpdate("DELETE FROM SurrogateNeighbors WHERE WebID=" + webid
				+ " AND SurrogateNeighbor=" + neighbor + ";");
	}
	/**
	 * Remove an inverse surrogate neighbor from a node
	 * Note that this is simply a reflexive operation to removeSurrogateNeighbor
	 * 
	 * @param webid the WebId of the parent node
	 * @return true, if the neighbor was successfully removed
	 * @author isaac
	 */
	protected boolean removeAllInverseSurrogateNeighbors(int webid) {
		return sqlUpdate("DELETE FROM SurrogateNeighbors WHERE SurrogateNeighbor="+webid+";");
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
