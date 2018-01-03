package controller;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import model.BreedGraph;

@Path("/breeds")
public class BreedsController {
	String uriBase = "http://localhost:8080/SemanticWebService/breeds/";

	@GET
	@Path("{id}/")
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getBreed(@PathParam("id") String id, @HeaderParam("Accept") String accept) throws Exception{
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		String file = BreedGraph.getBreed(uriBase+ id, format);
		if(file.equals("False"))
			return Response.status(404).build();
		else		
			return Response.ok(file).header("Content-Disposition",  "attachment; filename=\""+id+"."+format+"\" ").build();
	}

	// This method is called if XML is request
	@GET	
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getAll(@HeaderParam("Accept") String accept) throws Exception{		
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		String file = BreedGraph.getAll(format);		
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
	public Response postBreed(JsonObject json){			
		try {					
			String uri = BreedGraph.createBreed(expandJson(json));		
			if(uri == null) {
				return Response.status(406).entity("Não foi possível cadastrar uma raça com o nickname solicitado.").build();
			}
			return Response.status(201).entity(uri).build();
		}	
		catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			

	}

	@PUT
	@Consumes("application/json")	
	@Path("{id}/")
	public Response putBreed(@PathParam("id") String id, JsonObject json)  {
		try {			
			BreedGraph.updateBreedGraph(uriBase + id , expandJson(json));
			return Response.status(200).build();
		}	
		catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}		
	}


	@DELETE
	@Path("{id}/")	
	public Response deleteBreed(@PathParam("id")String id) {		
		boolean exist;
		try {
			exist = BreedGraph.deleteBreedGraph(uriBase + id);
			if(exist)
				return Response.status(200).build();
			else
				return Response.status(404).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
	}



	private JsonObject expandJson(JsonObject obj){
		JsonBuilderFactory factory = Json.createBuilderFactory(null);
		JsonObjectBuilder jsonOB = factory.createObjectBuilder();

		Iterator<Entry<String, JsonValue>> iterator = obj.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, JsonValue> entry = iterator.next();
			String prefix = setPrefix(entry.getKey().toString());
			jsonOB.add(prefix + entry.getKey(), entry.getValue().toString().substring(1, entry.getValue().toString().length()-1));
		}		


		return jsonOB.build();
	}

	public String setPrefix(String property) {
		String prefix = "";
		if (property.equals("label") || property.equals("comment") ) {
			prefix = "http://www.w3.org/2000/01/rdf-schema#";
		}else if (property.equals("id")){
			prefix = "";
		}else{
			prefix = "http://www.w3.org/2004/02/skos/core#";
		}
		return prefix;
	}

}
