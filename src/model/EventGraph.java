package model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
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
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import util.JenaSesameUtils;

public class EventGraph {
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
		Model EventModel = ModelFactory.createDefaultModel();
		EventModel.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		EventModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		EventModel.setNsPrefix("event", "http://purl.org/NET/c4dm/event.owl#");
		EventModel.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		EventModel.setNsPrefix("owl-time", "https://www.w3.org/TR/owl-time/#time:");
		EventModel.setNsPrefix("time", "http://www.w3.org/2006/time#");
		
		boolean exist = false;
		String queryString = "Select ?s"						
				+ " WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/NET/c4dm/event.owl#Event> }";
		
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
				Statement statement = JenaSesameUtils.asJenaStatement(solution);
				EventModel.add(statement);
			 }
	   	
	   }
		conn.close();
		combinedRepo.close();
		if(!exist)
			return null;
		OutputStream stream = new ByteArrayOutputStream() ;					
		if(format.equals("rdf"))						
			EventModel.write(stream, "RDF/XML-ABBREV");				
		if(format.equals("ttl"))				
			EventModel.write(stream, "TURTLE");								
		return stream.toString();				

	}
	
	public static String getEvent(String resourceURI, String extensao) throws Exception {
		ConnectCombinedRepository();
		Model fakeModel = ModelFactory.createDefaultModel();
		fakeModel.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		fakeModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		fakeModel.setNsPrefix("event", "http://purl.org/NET/c4dm/event.owl#");
		fakeModel.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		fakeModel.setNsPrefix("owl-time", "https://www.w3.org/TR/owl-time/#time:");
		fakeModel.setNsPrefix("time", "http://www.w3.org/2006/time#");
		
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
		
		conn.close();
		combinedRepo.close();
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

	public static String createEvent(JsonObject json) throws Exception {
		AGGraph graph = ConnectDataRepository();						
		AGModel model = new AGModel(graph);
		
		Iterator<?> subjects = model.listSubjects();
		Iterator<Entry<String, JsonValue>> json_values = json.entrySet().iterator();
		
		id = newId(json_values);
		if (existId(subjects, id) || (id.equals(""))) {
			return null;
		}
		
		Resource resource =	model.createResource("http://localhost:8080/SemanticWebService/events/" + id);
		Property type = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource resourceLocation =	model.createResource("http://purl.org/NET/c4dm/event.owl#Event");
		model.add(resource, type, resourceLocation);
		Iterator<Entry<String, JsonValue>> iterator = json.entrySet().iterator();			
		while (iterator.hasNext()) {
			Entry<String, JsonValue> entry = iterator.next();
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			JsonValue Jvalue = entry.getValue();
			
			if(!value.equals("") && !key.equals("id")) {
				if (key.equals("http://purl.org/NET/c4dm/event.owl#agent")){
					JSONArray arrayAgent = new JSONArray("["+Jvalue.toString()+"]");
					
					for (int i = 0; i < arrayAgent.length(); i++) {
					    
					    String agent = arrayAgent.getString(i);
					     
				    	Resource uriAgent = model.createResource(agent);
				    	model.add(resource, model.getProperty(entry.getKey()), uriAgent);
					}
				}
				else if (key.equals("http://purl.org/NET/c4dm/event.owl#place")) {
					Resource resourcePropertyLocation = model.createResource(value.substring(1,value.length()-1));
					model.add(resource, model.getProperty(entry.getKey()), resourcePropertyLocation);
				}
				else if (key.equals("http://purl.org/NET/c4dm/event.owl#time")){
				 	Resource node = model.createResource();
				 	Property predicate = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				 	Resource object = model.createResource("https://www.w3.org/TR/owl-time/#time:Instant");
				 	
				 	Property timeinstant = model.createProperty("https://www.w3.org/TR/owl-time/#time:inXSDDateTimeStamp");
				 	Literal datetime = model.createTypedLiteral(value.substring(1,value.length()-1), "http://www.w3.org/2001/XMLSchema#dateTimeStamp");
				 	node.addProperty(predicate, object);
				 	node.addProperty(timeinstant, datetime);
				 						                           
					model.add(resource, model.getProperty(entry.getKey()), node);
				} else if (key.equals("http://purl.org/NET/c4dm/event.owl#product")){
					String arrayString = Jvalue.toString();
					arrayString = arrayString.substring(1, arrayString.length()-1);
					arrayString = arrayString.replace("\\", "");
					JSONArray array = new JSONArray("["+arrayString+"]");
					
					for (int i = 0; i < array.length(); i++) {
					    JSONObject row = array.getJSONObject(i);
					    String id_img = row.getString("id");
					    String description = row.getString("description");
				    	Resource img = model.createResource("http://localhost:8080/SemanticWebService/events/" +
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

	public static void updateEventGraph(String uri, JsonObject json) throws Exception{			
		AGGraph graph = ConnectDataRepository();			
		AGModel model = new AGModel(graph);			
		Model addModel = ModelFactory.createDefaultModel();						
		Resource resource =	addModel.createResource(uri);
		removeTriples(model, uri, "images"); // o primeiro argumento é o model, o segundo argumento são todos os sujeitos que serão removidos, e o terceiro argumento é qual predicado não será excluído
							
		Iterator<Entry<String, JsonValue>> iterator = json.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, JsonValue> entry = iterator.next();
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();
			JsonValue Jvalue = entry.getValue();
			
			
			if(!value.equals("") && !key.equals("id")) {
				if (key.equals("http://purl.org/NET/c4dm/event.owl#agent")){
					String arrayString = Jvalue.toString();
					JSONArray arrayAgent = new JSONArray("["+arrayString+"]");
					
					for (int i = 0; i < arrayAgent.length(); i++) {
					    
					    String agent = arrayAgent.getString(i);
					     
				    	Resource uriAgent = model.createResource(agent);
				    	model.add(resource, model.getProperty(entry.getKey()), uriAgent);
					}
				}
				else if (key.equals("http://purl.org/NET/c4dm/event.owl#place")) {
					Resource resourceLocation = model.createResource(value.substring(1,value.length()-1));
					model.add(resource, model.getProperty(entry.getKey()), resourceLocation);
				} else if (key.equals("http://purl.org/NET/c4dm/event.owl#time")){
				 	Resource node = model.createResource();
				 	Property predicate = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
				 	Resource object = model.createResource("https://www.w3.org/TR/owl-time/#time:Instant");
				 	
				 	Property timeinstant = model.createProperty("https://www.w3.org/TR/owl-time/#time:inXSDDateTimeStamp");
				 	Literal datetime = model.createTypedLiteral(value.substring(1,value.length()-1), "http://www.w3.org/2001/XMLSchema#dateTimeStamp");
				 	node.addProperty(predicate, object);
				 	node.addProperty(timeinstant, datetime);
				 						                           
					model.add(resource, model.getProperty(entry.getKey()), node);
				} else if (key.equals("http://purl.org/NET/c4dm/event.owl#product")){
					String arrayString = Jvalue.toString();
					arrayString = arrayString.substring(1, arrayString.length()-1);
					arrayString = arrayString.replace("\\", "");
					JSONArray array = new JSONArray("["+arrayString+"]");
					
					String ids[] = new String[array.length()];
					String Id = uri.toString().substring(48);
					for (int i = 0; i < array.length(); i++) {
					    JSONObject row = array.getJSONObject(i);
					    String id_img = row.getString("id");
					    String description = row.getString("description");
					    
					    ids[i] = id_img;
					    
				    	Resource img = model.createResource("http://localhost:8080/SemanticWebService/events/" +
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
		Resource resourceLocation =	model.createResource("http://purl.org/NET/c4dm/event.owl#Event");
		model.add(resource, type, resourceLocation);			
		model.add(addModel);
		conn.close();
	}
	
	private static void DeleteSomeImages(AGModel model, String id_user, String[] ids) {
		String ResourceURI = "http://localhost:8080/SemanticWebService/events/" + id_user;
		String id_events = ResourceURI.toString().substring(48);
		
		String folder_path = "images/" + "events/" + id_events;
		
		File folder = new File(folder_path);
		File[] listOfFiles = folder.listFiles();
		try {
			for (int i = 0; i < listOfFiles.length; i++) {
				if (!Arrays.asList(ids).contains(listOfFiles[i].getName())){
					String image_path = "images/" + "events/" + id_user + "/" + listOfFiles[i].getName();
					File file = new File(image_path);
					file.delete();
				}
			}	
		} catch (Exception e) {
		}
	}
		

	public static boolean deleteEventGraph(String resourceURI) throws Exception{		
		AGGraph graph = ConnectDataRepository();			
		AGModel model = new AGModel(graph);
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
	
		String file = EventGraph.getEvent(imageURI, extensao);
		return file;
	}

	public static void createImage(InputStream uploadedInputStream,
			String path_folder,
			String filename, String format) {

		BufferedImage image;
        try {
        	File file = new File("images/" + "events/" + path_folder + "/" + filename);
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
					if(st.getPredicate().getURI().equals("http://purl.org/NET/c4dm/event.owl#product") && !except.equals("images")) {
						removeImages(model, uri, st.getObject().toString());
					}else if (st.getPredicate().getURI().equals("http://purl.org/NET/c4dm/event.owl#product") && except.equals("images")) {
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
		String id_events = ResourceURI.toString().substring(48);
		String folder_path = "images/" + "events/" + id_events;
		try {
			FileUtils.deleteDirectory(new File(folder_path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Boolean existId(Iterator<?> subjects, String id){
		while(subjects.hasNext()){								
			Resource r = (Resource) subjects.next();			
			if(r.toString().contains("http://localhost:8080/SemanticWebService/events/")){/*Verify if it's the ontology statement*/
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