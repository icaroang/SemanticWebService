package test;

import static org.junit.Assert.*;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.junit.Test;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;

import controller.PeopleController;
import model.PersonGraph;

public class PersonGraphTest {

	private String rdf;

	@Test
	public void testConnectSingleRepository() throws Exception {
//		  AGGraph graph = PersonGraph.ConnectSingleRepository();
//		  assertEquals(graph, graph);	
		
		  PersonGraph person = new PersonGraph();
		  rdf = PersonGraph.getAll("application/rdf+xml");
		  System.out.println(rdf);
		  
//		  ClientConfig config = new ClientConfig();
//		  Client client = ClientBuilder.newClient(config);
//		  WebTarget service = client.target(getBaseURI());
//
//	   
//	      // Get the Todos
//	      System.out.println(service.path("people").request().accept(MediaType.TEXT_XML).get(String.class));

//	    // Get JSON for application
//	    System.out.println(service.path("rest").path("todos").request().accept(MediaType.APPLICATION_JSON).get(String.class));

//	      // Get XML for application
//	      System.out.println(service.path("rest").path("todos").request().accept(MediaType.APPLICATION_XML).get(String.class));
//
//	      //Get Todo with id 1
//	      Response checkDelete = service.path("rest").path("todos/1").request().accept(MediaType.APPLICATION_XML).get();
//
//	      //Delete Todo with id 1
//	      service.path("rest").path("todos/1").request().delete();
//
//	      //Get get all Todos id 1 should be deleted
//	      System.out.println(service.path("rest").path("todos").request().accept(MediaType.APPLICATION_XML).get(String.class));
//
//	      //Create a Todo
//	      Form form =new Form();
//	      form.param("id", "4");
//	      form.param("summary","Demonstration of the client lib for forms");
//	      response = service.path("rest").path("todos").request().post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED),Response.class);
//	      System.out.println("Form response " + response.getStatus());
//
//	      //Get all the todos, id 4 should have been created
//	      System.out.println(service.path("rest").path("todos").request().accept(MediaType.APPLICATION_XML).get(String.class));

	  }

	  private static URI getBaseURI() {
	    return UriBuilder.fromUri("http://localhost:8080/SemanticWebService").build();
	  }
	}
