package model;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;
import com.franz.agraph.jena.AGModel;
import com.franz.agraph.repository.AGCatalog;
import com.franz.agraph.repository.AGRepository;
import com.franz.agraph.repository.AGRepositoryConnection;
import com.franz.agraph.repository.AGServer;
import com.franz.agraph.repository.AGVirtualRepository;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import util.JenaSesameUtils;

public class PlaceGraph {
	public static String SERVER_URL = "http://localhost:10035";
	public static String CATALOG_ID = "java-catalog";
	public static String REPOSITORY_ID = "data";
	public static String USERNAME = "icaroang";
	public static String PASSWORD = "icaro123";
	public static String TEMPORARY_DIRECTORY = "";
	private static AGVirtualRepository combinedRepo;
	private static AGRepositoryConnection conn;
	private static AGGraphMaker maker;
	private static String id = "";

	public static AGGraph ConnectDataRepository() throws Exception {			

		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGCatalog catalog = server.getCatalog(CATALOG_ID);

		AGRepository myRepository = catalog.createRepository(REPOSITORY_ID);
		println("Got a repository.");
		myRepository.initialize();
		println("Initialized repository.");
		conn = myRepository.getConnection();
		closeBeforeExit(conn);
		println("Got a connection.");
		println("Repository " + (myRepository.getRepositoryID()) + " is up! It contains " + (conn.size())
				+ " statements.");
		maker = new AGGraphMaker(conn);
		AGGraph graph = maker.getGraph();
		return graph;
	}

	private static void ConnectCombinedRepository() throws Exception {			
		println("\nConnectCombinedRepository.");
		AGServer server = new AGServer(SERVER_URL, USERNAME, PASSWORD);
		AGRepository ontologiesRespository = server.getCatalog(CATALOG_ID).openRepository("ontologies");
		AGRepository dataRespository = server.getCatalog(CATALOG_ID).openRepository("data");

		combinedRepo = server.federate(ontologiesRespository, dataRespository);

		combinedRepo.initialize();
		conn = combinedRepo.getConnection();		
	}

	public static String getAll(String format) throws Exception{
		ConnectCombinedRepository();
		Model PlaceModel = ModelFactory.createDefaultModel();
		PlaceModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		PlaceModel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		PlaceModel.setNsPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
		boolean exist = false;
		String queryString = "Select ?s ?p ?o "						
				+ " WHERE { ?s ?p ?o.filter regex(str(?s), \"places/\", \"i\")}";

		closeBeforeExit(conn);

		conn.begin();

		TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		tupleQuery.setIncludeInferred(true);
		TupleQueryResult results = tupleQuery.evaluate();

		while (results.hasNext()) {   	
			exist = true;

			BindingSet result = results.next();

			String queryDescribeString;
			queryDescribeString = "Describe <"+result.getValue("s").toString()+"> ?s ?p ?o ";

			GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryDescribeString);
			describeQuery.setIncludeInferred(true);
			GraphQueryResult resultDescribe = describeQuery.evaluate();
			while(resultDescribe.hasNext()){
				org.openrdf.model.Statement solution = resultDescribe.next();						
				Statement statement = JenaSesameUtils.asJenaStatement(solution);
				PlaceModel.add(statement);			 
				exist = true;
			}
		}

		conn.close();
		combinedRepo.close();
		if(!exist)
			return null;
		OutputStream stream = new ByteArrayOutputStream() ;					
		if(format.equals("rdf"))						
			PlaceModel.write(stream, "RDF/XML-ABBREV");				
		if(format.equals("ttl"))				
			PlaceModel.write(stream, "TURTLE");								
		return stream.toString();				

	}

	public static String getPlace(String resourceURI, String extensao) throws Exception {
		ConnectCombinedRepository();
		Model fakeModel = ModelFactory.createDefaultModel();
		fakeModel.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		fakeModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		fakeModel.setNsPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");

		boolean exist = false;

		String queryString;
		queryString = "Describe <"+resourceURI+"> ?s ?p ?o ";

		GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
		describeQuery.setIncludeInferred(true);
		GraphQueryResult  result = describeQuery.evaluate();
		while(result.hasNext()){
			org.openrdf.model.Statement solution = result.next();						
			Statement statement = JenaSesameUtils.asJenaStatement(solution);
			fakeModel.add(statement);			 
			exist = true;
		}

		if(exist){
			OutputStream stream = new ByteArrayOutputStream() ;					
			if(extensao.equals("rdf"))						
				fakeModel.write(stream, "RDF/XML-ABBREV");				
			if(extensao.equals("ttl")){					
				fakeModel.write(stream, "TURTLE");
			}				
			return stream.toString();
		}
		else
			return "False";
	}

	public static String createPlace(JsonObject json) throws Exception {
		AGGraph graph = ConnectDataRepository();						
		AGModel model = new AGModel(graph);

		Iterator<?> subjects = model.listSubjects();
		Iterator<Entry<String, JsonValue>> json_values = json.entrySet().iterator();

		id = newId(json_values);
		if (existId(subjects, id) || (id.equals(""))) {
			return null;
		}

		Resource resource =	model.createResource("http://localhost:8080/SemanticWebService/places/" + id);
		Property type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource resourceLocation =	model.createResource("http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing");
		model.add(resource, type, resourceLocation);
		Iterator<Entry<String, JsonValue>> iterator = json.entrySet().iterator();			
		while (iterator.hasNext()) {
			Entry<String, JsonValue> entry = iterator.next();
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();

			if(!value.equals("") && !key.equals("id")) {
				model.add(resource, model.getProperty(entry.getKey()), value.substring(1,value.length()-1));	
			}						
		}				

		conn.close();
		return resource.getURI();

	}	

	public static void updatePlaceGraph(String uri, JsonObject json) throws Exception{			
		AGGraph graph = ConnectDataRepository();			
		AGModel model = new AGModel(graph);			
		Model addModel = ModelFactory.createDefaultModel();						
		Resource resource =	addModel.createResource(uri);
		removeTriples(model, uri, "");

		Iterator<Entry<String, JsonValue>> iterator = json.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, JsonValue> entry = iterator.next();
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();

			if(!value.equals("") && !key.equals("id")) {
				model.add(resource, model.getProperty(entry.getKey()), value.substring(1,value.length()-1));	
			}						
		}	

		Property type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource resourceLocation =	model.createResource("http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing");
		model.add(resource, type, resourceLocation);			
		model.add(addModel);
		conn.close();
	}


	public static boolean deletePlaceGraph(String resourceURI) throws Exception{		
		AGGraph graph = ConnectDataRepository();			
		AGModel model = new AGModel(graph);
		boolean exist = false;
		exist = removeTriples(model, resourceURI, "");
		conn.close();
		return exist;
	}


	private static boolean removeTriples(AGModel model, String uri, String except) {
		StmtIterator result = model.listStatements();
		boolean exist = false;
		while (result.hasNext()) {				
			Statement st = result.next();	
			if(!st.getSubject().isAnon()){
				if(st.getSubject().getURI().equals(uri)) {
					if (!except.isEmpty() && !st.getPredicate().getURI().equals(except)){					
						if(st.getObject().isAnon())							
							model.removeAll((Resource)st.getObject(), (Property)null, (RDFNode)null);
						model.remove(st);
					}else if (except.isEmpty()) {
						if(st.getObject().isAnon())							
							model.removeAll((Resource)st.getObject(), (Property)null, (RDFNode)null);
						model.remove(st);
					}
					exist = true;
				}
			}
		}
		return exist;
	}


	private static Boolean existId(Iterator<?> subjects, String id){
		while(subjects.hasNext()){								
			Resource r = (Resource) subjects.next();			
			if(r.toString().contains("http://localhost:8080/SemanticWebService/places/")){/*Verify if it's the ontology statement*/
				String existId = r.toString().substring(48);	
				if (existId.equals(id)) {
					return true;
				}
			}				
		}
		return false;
	}

	private static String newId (Iterator<Entry<String, JsonValue>> json_values) {
		String newId = "";
		while(json_values.hasNext())
		{
			Entry<String, JsonValue> entry = json_values.next();
			if (entry.getKey().toString().equals("id")){
				String id = entry.getValue().toString();
				newId = id.substring(1,id.length()-1);
			}
		}
		return newId;
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
