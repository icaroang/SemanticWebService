package controller;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
}
