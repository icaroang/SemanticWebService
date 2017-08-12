package controller;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.riot.RDFFormat;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.openrdf.rio.Rio;

import com.franz.agraph.jena.AGGraph;
import com.hp.hpl.jena.rdf.model.Model;

import jena.turtle;
import model.PersonGraph;

@Path("/people")
public class PeopleController {
	

	// This method is called if XML is request
	@GET	
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getAll(@HeaderParam("Accept") String accept) throws Exception{		
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		String file = PersonGraph.getAll(format);		
		if(file == null)
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\"all"+"."+format+"\" ").build();
	  }

	// This method is called if HTML is request
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHtmlHello() {
	  return "<html> " + "<title>" + "Hello Jersey" + "</title>"
	      + "<body><h1>" + "Hello Jersey" + "</body></h1>" + "</html> ";
	}
	
	@POST
	@Consumes("application/json")
	public Response postPerson(JsonObject json){			
		try {					
			String uri = PersonGraph.createPerson(expandJson(json));			
			return Response.status(201).entity(uri).build();
		}	
		 catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
		
	}
	
	
	private JsonObject expandJson(JsonObject obj){
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();
	
			Iterator iterator = obj.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<String, JsonValue> entry = (Entry<String, JsonValue>)iterator.next();
				jsonOB.add(entry.getKey(), entry.getValue().toString().substring(1, entry.getValue().toString().length()-1));
			}		
		
		
		return jsonOB.build();
	}
		
}
