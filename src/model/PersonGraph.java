package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.io.parser.JSONParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

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
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import jena.turtle;
import util.JenaSesameUtils;

public class PersonGraph {
	public static String SERVER_URL = "http://localhost:10035";
	public static String CATALOG_ID = "java-catalog";
	public static String REPOSITORY_ID = "data";
	public static String USERNAME = "icaroang";
	public static String PASSWORD = "icaro123";
	public static String TEMPORARY_DIRECTORY = "";
	private static AGVirtualRepository combinedRepo;
	private static AGRepositoryConnection conn;
	private static AGGraphMaker maker;
	private static AGRepository repository;
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
		Model PeopleModel = ModelFactory.createDefaultModel();
		boolean exist = false;
		String queryString = "Select ?s"						
				+ " WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> }";
		
		closeBeforeExit(conn);

		conn.begin();
	
		TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		tupleQuery.setIncludeInferred(false);
		TupleQueryResult results = tupleQuery.evaluate();
		   
		while (results.hasNext()) {   	
			exist = true;
			
			BindingSet result= results.next();
			println(result.getValue("s").toString());
			
			String queryDescribeString;
			queryDescribeString = "Describe <"+result.getValue("s").toString()+"> ?s ?p ?o ";
		
			GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryDescribeString);
			describeQuery.setIncludeInferred(true);
			GraphQueryResult resultDescribe = describeQuery.evaluate();
			while(resultDescribe.hasNext()){
				org.openrdf.model.Statement solution = resultDescribe.next();						
				JenaSesameUtils convert = new JenaSesameUtils();
				Statement statement = convert.asJenaStatement(solution);
				PeopleModel.add(statement);			 
				exist = true;				 
			 }
	   	
	   }
		conn.close();
		combinedRepo.close();
		if(!exist)
			return null;
		OutputStream stream = new ByteArrayOutputStream() ;					
		if(format.equals("rdf"))						
			PeopleModel.write(stream, "RDF/XML-ABBREV");				
		if(format.equals("ttl"))				
			PeopleModel.write(stream, "TURTLE");								
		return stream.toString();				

	}
	
	public static String getPerson(String resourceURI, String extensao) throws Exception {
		ConnectCombinedRepository();
		Model fakeModel = ModelFactory.createDefaultModel();
		fakeModel.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		fakeModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		
		boolean exist = false;
		
		String queryString;
		queryString = "Describe <"+resourceURI+"> ?s ?p ?o ";
	
		GraphQuery describeQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
		describeQuery.setIncludeInferred(true);
		GraphQueryResult  result = describeQuery.evaluate();
		while(result.hasNext()){
			org.openrdf.model.Statement solution = result.next();						
			JenaSesameUtils convert = new JenaSesameUtils();
			Statement statement = convert.asJenaStatement(solution);
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

	public static String createPerson(JsonObject json) throws Exception {
		AGGraph graph = ConnectDataRepository();						
		AGModel model = new AGModel(graph);
		
		Iterator subjects = model.listSubjects();
		Iterator json_values = json.entrySet().iterator();
		
		id = newId(json_values);
		if (existId(subjects, id) || (id.equals(""))) {
			return null;
		}
		
		Resource resource =	model.createResource("http://localhost:8080/SemanticWebService/people/" + id);
		Property type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource resourceLocation =	model.createResource("http://xmlns.com/foaf/0.1/Person" );
		model.add(resource, type, resourceLocation);
		Iterator iterator = json.entrySet().iterator();			
		while (iterator.hasNext()) {
			Entry<String, JsonString> entry= (Entry<String, JsonString>)iterator.next();
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			JsonString Jvalue = entry.getValue();
			
			if(!value.equals("") && !key.equals("id")) {
				if (key.equals("http://xmlns.com/foaf/0.1/knows")) {
					Resource peopleLocation = model.createResource(value.substring(1,value.length()-1));
					model.add(resource, model.getProperty(entry.getKey()), peopleLocation);
				} else if (key.equals("http://xmlns.com/foaf/0.1/img")){
					JSONArray array = new JSONArray("["+Jvalue.getString()+"]");
					for (int i = 0; i < array.length(); i++) {
					    JSONObject row = array.getJSONObject(i);
					    String id_img = row.getString("id");
					    String description = row.getString("description");
				    	Resource img = model.createResource("http://localhost:8080/SemanticWebService/people/" +
				    	id + "/images/" + id_img);
				    	model.add(resource, model.getProperty(entry.getKey()), img);
				    	model.add(img, model.getProperty("http://www.w3.org/2000/01/rdf-schema#comment"), description);
					}
				} else {
					model.add(resource, model.getProperty(entry.getKey()), value.substring(1,value.length()-1));	
				}
			}						
		}				

		conn.close();
		return resource.getURI();
	
	}	

	public static void updatePersonGraph(String uri, JsonObject json) throws Exception{			
		AGGraph graph = ConnectDataRepository();			
		AGModel model = new AGModel(graph);			
		Model addModel = ModelFactory.createDefaultModel();						
		Resource resource =	addModel.createResource(uri);
		removeTriples(model, uri, "images"); // o primeiro argumento é o model, o segundo argumento são todos os sujeitos que serão removidos, e o terceiro argumento é qual predicado não será excluído
							
		Iterator iterator = json.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, JsonString> entry= (Entry<String, JsonString>)iterator.next();
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			JsonString Jvalue = entry.getValue();
		
			if(!value.equals("") && !key.equals("id")) {
				if (key.equals("http://xmlns.com/foaf/0.1/knows")) {
					Resource peopleLocation = model.createResource(value.substring(1,value.length()-1));
					model.add(resource, model.getProperty(entry.getKey()), peopleLocation);
				} else if (key.equals("http://xmlns.com/foaf/0.1/img")){
					JSONArray array = new JSONArray("["+Jvalue.getString()+"]");
					String ids[] = new String[array.length()];
					String Id = uri.toString().substring(48);
					for (int i = 0; i < array.length(); i++) {
					    JSONObject row = array.getJSONObject(i);
					    String id_img = row.getString("id");
					    String description = row.getString("description");
					    
					    ids[i] = id_img;
					    
				    	Resource img = model.createResource("http://localhost:8080/SemanticWebService/people/" +
				    	Id + "/images/" + id_img);
				    	model.add(resource, model.getProperty(entry.getKey()), img);
				    	model.add(img, model.getProperty("http://www.w3.org/2000/01/rdf-schema#comment"), description);
					}
					DeleteSomeImages(model, Id, ids);

				} else {
					model.add(resource, model.getProperty(entry.getKey()), value.substring(1,value.length()-1));	
				}
			}						
		}	

		Property type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource resourceLocation =	model.createResource("http://xmlns.com/foaf/0.1/Person" );
		model.add(resource, type, resourceLocation);			
		model.add(addModel);
		conn.close();
	}
	
	private static void DeleteSomeImages(AGModel model, String id_user, String[] ids) {
		String ResourceURI = "http://localhost:8080/SemanticWebService/people/" + id_user;
		String id_people = ResourceURI.toString().substring(48);
		
		String folder_path = "images/" + "people/" + id_people;
		
		File folder = new File(folder_path);
		File[] listOfFiles = folder.listFiles();
		
		for (int i = 0; i < listOfFiles.length; i++) {
			if (!Arrays.asList(ids).contains(listOfFiles[i].getName())){
				String image_path = "images/" + "people/" + id_user + "/" + listOfFiles[i].getName();
				File file = new File(image_path);
				file.delete();
			}
		}
	}
		

	public static boolean deletePersonGraph(String resourceURI) throws Exception{		
		AGGraph graph = ConnectDataRepository();			
		AGModel model = new AGModel(graph);
		StmtIterator iterator = model.listStatements();	
		boolean exist = false;
		exist = removeTriples(model, resourceURI, "");
		conn.close();
		return exist;
	}
	
	public static String getImage(String resourceURI, String imageURI, String extensao) throws Exception {
//		ConnectCombinedRepository();
//		Model fakeModel = ModelFactory.createDefaultModel();
//		fakeModel.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
//		fakeModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
//		
//		String queryImgString;
//		queryImgString = "Describe <"+imageURI+"> ?s ?p ?o ";
//	
//		GraphQuery describeImgQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryImgString);
//		describeImgQuery.setIncludeInferred(true);
//		GraphQueryResult resultImg = describeImgQuery.evaluate();
//		println(resultImg);
//		if (!resultImg.hasNext()) {
//			return "False";
//		}
	
		String file = PersonGraph.getPerson(imageURI, extensao);
		return file;
	}

	public static void createImage(InputStream uploadedInputStream,
			String path_folder,
			String filename, String format) {

		BufferedImage image;
        try {
        	File file = new File("images/" + "people/" + path_folder + "/" + filename);
        	file.mkdirs();
        	
        	image = ImageIO.read(uploadedInputStream);
        	ImageIO.write(image, format, file);

        } catch (IOException e) {
        	e.printStackTrace();
        }
        System.out.println("Done");
    }
	
	private static boolean removeTriples(AGModel model, String uri, String except) {
		StmtIterator result = model.listStatements();
		boolean exist = false;
		while (result.hasNext()) {				
			Statement st = result.next();	
			if(!st.getSubject().isAnon()){
				if(st.getSubject().getURI().equals(uri)) {
					if(st.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/img") && !except.equals("images")) {
						removeImages(model, uri, st.getObject().toString());
					}else if (st.getPredicate().getURI().equals("http://xmlns.com/foaf/0.1/img") && except.equals("images")) {
						removeImagesTriples(model, uri, st.getObject().toString());
					}
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
	
	
	private static void removeImagesTriples(AGModel model, String ResourceURI, String ResourceImageURI) {
		StmtIterator result = model.listStatements();
		while (result.hasNext()) {				
			Statement st = result.next();	
			if(!st.getSubject().isAnon()){
				if(st.getSubject().getURI().equals(ResourceImageURI)) {
					model.remove(st);
					}
				}
		}
	}

	private static void removeImages(AGModel model, String ResourceURI, String ResourceImageURI) {
		
		StmtIterator result = model.listStatements();
		while (result.hasNext()) {				
			Statement st = result.next();	
			if(!st.getSubject().isAnon()){
				if(st.getSubject().getURI().equals(ResourceImageURI)) {
					model.remove(st);
					}
				}
		}
		String id_people = ResourceURI.toString().substring(48);
		String id_image = ResourceImageURI.toString().substring(64);
		String folder_path = "images/" + "people/" + id_people;
		String image_path = "images/" + "people/" + id_people + "/" + id_image;
		
		try {
			FileUtils.deleteDirectory(new File(folder_path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Boolean existId(Iterator subjects, String id){
		while(subjects.hasNext()){								
			Resource r = (Resource) subjects.next();			
			if(r.toString().contains("http://localhost:8080/SemanticWebService/people/")){/*Verify if it's the ontology statement*/
				String existId = r.toString().substring(48);	
				if (existId.equals(id)) {
					return true;
				}
			}				
		}
		return false;
	}
	
	private static String newId (Iterator json_values) {
		String newId = "";
		while(json_values.hasNext())
		{
			Entry<String, JsonValue> entry = (Entry<String, JsonValue>)json_values.next();
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
	
	private static JsonObject expandJson(JsonObject value){
		//JSONObject jsonObject = new JSONObject(value.toString());
		
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();
	
			Iterator iterator = value.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<String, JsonValue> entry = (Entry<String, JsonValue>)iterator.next();
				String prefix = setPrefix(entry.getKey().toString());
				jsonOB.add(entry.getKey(), entry.getValue().toString().substring(1, entry.getValue().toString().length()-1));
			}		
	
		
		return jsonOB.build();
	}
	
	public static String setPrefix(String property) {
		String prefix = "";
		if (property.equals("label") || property.equals("comment") ) {
			prefix = "http://www.w3.org/2000/01/rdf-schema#";
		}else if (property.equals("id")){
			prefix = "";
		}else{
			prefix = "http://xmlns.com/foaf/0.1/";
		}
		return prefix;
	}
	

}
