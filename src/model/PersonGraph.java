package model;

import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map.Entry;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.jena.AGQuery;
import com.franz.agraph.jena.AGQueryExecutionFactory;
import com.franz.agraph.jena.AGQueryFactory;
import com.franz.agraph.repository.AGBooleanQuery;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGVirtualRepository;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class PersonGraph {
	public static String SERVER_URL = "http://localhost:10035";
	public static String CATALOG_ID = "java-catalog";
	public static String REPOSITORY_ID = "teste";
	public static String USERNAME = "icaroang";
	public static String PASSWORD = "icaro123";
	public static String TEMPORARY_DIRECTORY = "";
	
	public static AGGraph ConnectSingleRepository() throws Exception {			
		println("\nStarting example1().");
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGCatalog catalog = server.getCatalog(CATALOG_ID);
		
		AGRepository myRepository = catalog.createRepository(REPOSITORY_ID);
		println("Got a repository.");
		myRepository.initialize();
		println("Initialized repository.");
		AGRepositoryConnection conn = myRepository.getConnection();
		closeBeforeExit(conn);
		println("Got a connection.");
		println("Repository " + (myRepository.getRepositoryID()) + " is up! It contains " + (conn.size())
				+ " statements.");
		AGGraphMaker maker = new AGGraphMaker(conn);
		AGGraph graph = maker.getGraph();
		return graph;
	}
	

	
	public static void println(Object x) {
		System.out.println(x);
	}
	protected static void closeBeforeExit(AGRepositoryConnection conn) {
		toClose.add(conn);
	}
	
	private static List<AGRepositoryConnection> toClose = new ArrayList<AGRepositoryConnection>();
	

	protected static void closeAll() {
		while (toClose.isEmpty() == false) {
			AGRepositoryConnection conn = toClose.get(0);
			close(conn);
			while (toClose.remove(conn)) {
			}
		}
	}
	
	static void close(AGRepositoryConnection conn) {
		try {
			conn.close();
		} catch (Exception e) {
			System.err.println("Error closing repository connection: " + e);
			e.printStackTrace();
		}
	}

}