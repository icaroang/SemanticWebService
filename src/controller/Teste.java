package controller;

import java.util.ArrayList;
import java.util.List;

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

public class Teste {

	public static String SERVER_URL = "http://localhost:10035";
	public static String CATALOG_ID = "java-catalog";
	public static String REPOSITORY_ID = "teste";
	public static String USERNAME = "icaroang";
	public static String PASSWORD = "icaro123";
	public static String TEMPORARY_DIRECTORY = "";

	static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

	public static AGGraphMaker example1(boolean close) throws Exception {
		println("\nStarting example1().");
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);

		try {
			println("Available catalogs: " + server.listCatalogs());
		} catch (Exception e) {
			throw new Exception("Got error when attempting to connect to server at " + SERVER_URL + ": " + e);
		}

		AGCatalog catalog = server.getCatalog(CATALOG_ID);

		if (catalog == null) {
			throw new Exception("Catalog " + CATALOG_ID + " does not exist. Either "
					+ "define this catalog in your agraph.cfg or modify the CATALOG_ID "
					+ "in this tutorial to name an existing catalog.");
		}

		println("Available repositories in catalog " + (catalog.getCatalogName()) + ": " + catalog.listRepositories());
		closeAll();
		catalog.deleteRepository(REPOSITORY_ID);
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
		println("Got a graph maker for the connection.");
		List<String> indices = conn.listValidIndices();
		println("All valid triple indices: " + indices);
		indices = conn.listIndices();
		println("Current triple indices: " + indices);
		println("Removing graph indices...");
		conn.dropIndex("gospi");
		conn.dropIndex("gposi");
		conn.dropIndex("gspoi");
		indices = conn.listIndices();
		println("Current triple indices: " + indices);
		println("Adding one graph index back in...");
		conn.addIndex("gspoi");
		indices = conn.listIndices();
		println("Current triple indices: " + indices);
		if (close) {
			// tidy up
			maker.close();
			conn.close();
			myRepository.shutDown();
			return null;
		}
		return maker;
	}

	public static AGModel example2(boolean close) throws Exception {
		AGGraphMaker maker = example1(false);
		AGGraph graph = maker.getGraph();
		AGModel model = new AGModel(graph);
		println("\nStarting example2().");
		// Create some resources and literals to make statements from.
		Resource alice = model.createResource("http://example.org/people/alice");
		Resource bob = model.createResource("http://example.org/people/bob");
		Property name = model.createProperty("http://example.org/ontology/name");
		Resource person = model.createResource("http://example.org/ontology/Person");
		Literal bobsName = model.createLiteral("Bob");
		Literal alicesName = model.createLiteral("Alice");
		println("Triple count before inserts: " + model.size());
		// Alice's name is "Alice"
		model.add(alice, name, alicesName);
		// Alice is a person
		model.add(alice, RDF.type, person);
		// Bob's name is "Bob"
		model.add(bob, name, bobsName);
		// Bob is a person, too.
		model.add(bob, RDF.type, person);
		println("Added four triples.");
		println("Triple count after inserts: " + (model.size()));
		StmtIterator result = model.listStatements();
		while (result.hasNext()) {
			Statement st = result.next();
			println(st);
		}
		model.remove(bob, name, bobsName);
		println("Removed one triple.");
		println("Triple count after deletion: " + (model.size()));
		// put it back so we can continue with other examples
		model.add(bob, name, bobsName);
		if (close) {
			model.close();
			graph.close();
			maker.close();
			return null;
		}
		return model;
	}

	/**
	 * A SPARQL Query
	 */
	public static void example3() throws Exception {
		AGModel model = example2(false);
		println("\nStarting example3().");
		try {
			String queryString = "SELECT ?s ?p ?o  WHERE {?s ?p ?o .}";
			AGQuery sparql = AGQueryFactory.create(queryString);
			QueryExecution qe = AGQueryExecutionFactory.create(sparql, model);
			try {
				ResultSet results = qe.execSelect();
				while (results.hasNext()) {
					QuerySolution result = results.next();
					RDFNode s = result.get("s");
					RDFNode p = result.get("p");
					RDFNode o = result.get("o");
					// System.out.format("%s %s %s\n", s, p, o);
					System.out.println(" { " + s + " " + p + " " + o + " . }");
				}
			} finally {
				qe.close();
			}
		} finally {
			model.close();
		}
	}

	public static void example4() throws Exception {
		AGRepositoryConnection conn = AGServer.createRepositoryConnection(REPOSITORY_ID, CATALOG_ID, SERVER_URL,
				USERNAME, PASSWORD);
		closeBeforeExit(conn);

		conn.begin();

		String updateString = "DELETE DATA { <http://example.org/people/alice>  <http://example.org/ontology/name>  \"Alice\" } ; \n"
				+ "\n"
				+ "INSERT DATA { <http://example.org/people/alice>  <http://example.org/ontology/name>  \"Ana\" }";
		println("\nPerforming SPARQL Update:\n" + updateString);
		conn.prepareUpdate(QueryLanguage.SPARQL, updateString).execute();
		String queryString = "ASK { <http://example.org/people/alice>  <http://example.org/ontology/name>  \"Ana\" }";
		println("\nPerforming query:\n" + queryString);
		println("Result: " + conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate());
		conn.commit();
	}

	public static void example5() throws Exception {
		AGRepositoryConnection conn = AGServer.createRepositoryConnection(REPOSITORY_ID, CATALOG_ID, SERVER_URL,
				USERNAME, PASSWORD);
		closeBeforeExit(conn);
		AGGraphMaker maker = new AGGraphMaker(conn);
		AGGraph graph = maker.getGraph();
		AGModel model = new AGModel(graph);

		conn.begin();

		String insertString = "INSERT DATA { <http://example.org/people/alice>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  foaf:Person }";
		println("\nPerforming SPARQL Insert:\n" + insertString);
		conn.prepareUpdate(QueryLanguage.SPARQL, insertString).execute();
		String queryString = "ASK { <http://example.org/people/alice>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  foaf:Person }";
		println("\nPerforming query:\n" + queryString);
		println("Result: " + conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate());

		println("");

		queryString = "ASK { <http://example.org/people/alice>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  foaf:Agent }";
		println("\nPerforming query:\n" + queryString);
		println("Result: " + conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString).evaluate());

		println("");

		queryString = "ASK { <http://example.org/people/alice>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  foaf:Agent }";
		AGBooleanQuery sparql = new AGBooleanQuery(conn, QueryLanguage.SPARQL, queryString, "");

		sparql.setEntailmentRegime(com.franz.agraph.repository.AGQuery.RESTRICTION);
		sparql.setIncludeInferred(true);

		// AGQueryExecution qe = AGQueryExecutionFactory.create(sparql, model);
		println("\nPerforming query:\n" + queryString);
		println("Result: " + sparql.evaluate());
		// println("Result: " + qe.execAsk());
		// Resource alice =
		// model.createResource("http://example.org/people/alice");
		// Property type =
		// model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		// printRows(infmodel.listStatements(alice, type, (RDFNode)null));
		conn.commit();
	}

	public static void example6() throws Exception {
		String serverURL = SERVER_URL + "/catalogs/" + CATALOG_ID + "/repositories/" + REPOSITORY_ID + "?infer=true";

		String queryString = "ASK { <http://example.org/people/alice>  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>  <http://xmlns.com/foaf/0.1/Agent> }";
		Query sparql = QueryFactory.create(queryString);
		// sparql.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

		QueryExecution qe = QueryExecutionFactory.sparqlService(serverURL, sparql);

		println("\n\nResult: " + qe.execAsk());
		qe.close();
	}

	public static void example7() throws Exception {
		String serverURL = SERVER_URL + "/catalogs/" + CATALOG_ID + "/repositories/" + REPOSITORY_ID + "?infer=true";
		// String serverURL =
		// "http://localhost:10035/session/44296/sessions/af1fcadb-bd76-a594-c98a-4b74631c4580?infer=true";

		String queryString = "SELECT ?p ?o  WHERE {<http://example.org/people/alice> ?p ?o .  }";
		Query sparql = QueryFactory.create(queryString);
		// sparql.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");

		QueryExecution qe = QueryExecutionFactory.sparqlService(serverURL, sparql);

		try {
			ResultSet results = qe.execSelect();
			ResultSetFormatter.out(System.out, results);
		} finally {
			qe.close();
		}
	}

	public static void example8() throws Exception {
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGVirtualRepository repo = server
				.virtualRepository("<java-catalog:teste>[restriction] + <java-catalog:teste2>[restriction]");
		AGRepositoryConnection combinedConn = repo.getConnection();
		closeBeforeExit(combinedConn);

		combinedConn.begin();

		String queryString = "SELECT ?p ?o  WHERE {<http://example.org/people/alice> ?p ?o .  }";
		TupleQuery query = combinedConn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		query.setIncludeInferred(false);
		TupleQueryResult results = query.evaluate();
		
		while (results.hasNext()) {
			BindingSet result = results.next();
			Value p = result.getValue("p");
			Value o = result.getValue("o");
			// System.out.format("%s %s %s\n", s, p, o);
			System.out.println(" {  " + p + " " + o + " . }");
		}
		combinedConn.commit();
		repo.close();
		server.close();
	}

	public static void println(Object x) {
		System.out.println(x);
	}

	protected static void closeBeforeExit(AGRepositoryConnection conn) {
		toClose.add(conn);
	}

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

	private static List<AGRepositoryConnection> toClose = new ArrayList<AGRepositoryConnection>();

	static void printRows(StmtIterator rows) throws Exception {
		while (rows.hasNext()) {
			println(rows.next());
		}
		rows.close();
	}

}
