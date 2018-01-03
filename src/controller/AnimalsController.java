package controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
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

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import model.AnimalGraph;

@Path("/animals")
public class AnimalsController {
	String uriBase = "http://localhost:8080/SemanticWebService/animals/";

	@GET
	@Path("{id}/")
	@Produces({"application/rdf+xml", "text/turtle"})
	public Response getAnimal(@PathParam("id") String id, @HeaderParam("Accept") String accept) throws Exception{
		String format = "rdf";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		String file = AnimalGraph.getAnimal(uriBase+ id, format);
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
		String file = AnimalGraph.getAll(format);		
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
	public Response postAnimal(JsonObject json){			
		try {					
			String uri = AnimalGraph.createAnimal(expandJson(json));			
			if(uri == null) {
				return Response.status(406).entity("Não foi possível cadastrar um animal com o nickname solicitado.").build();
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
	public Response putAnimal(@PathParam("id") String id, JsonObject json)  {
		try {			
			AnimalGraph.updateAnimalGraph(uriBase + id , expandJson(json));
			return Response.status(200).entity("Atualizado com sucesso").build();
		}	
		catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}		
	}


	@DELETE
	@Path("{id}/")	
	public Response deleteAnimal(@PathParam("id")String id) {		
		boolean exist;
		try {
			exist = AnimalGraph.deleteAnimalGraph(uriBase + id);
			if(exist)
				return Response.status(200).build();
			else
				return Response.status(404).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(304).build();
		}			
	}

	@GET
	@Path("{id}/images/{id_img}")
	@Produces({"image/jpeg", "image/jpg", "image/png", "image/gif",
		"image/svg+xml", "application/rdf+xml", "text/turtle"})

	public Response getImage(@PathParam("id") String id, @PathParam("id_img") String id_img, @HeaderParam("Accept") String accept) throws Exception{
		String format = "image";
		if(accept != null && accept.equals("text/turtle"))
			format = "ttl";
		else if(accept != null && accept.equals("application/rdf+xml")) {
			format = "rdf";
		}
		if (format.equals("image")) {
			String path = "images/" + "animals/" + id + "/" + id_img;
			File file = new File(path);
			if (!file.exists()) {
				return Response.status(404).build();
			}
			String ext = getFileExtension(id_img);
			BufferedImage saved_image = ImageIO.read(file);

			ByteArrayOutputStream final_image = new ByteArrayOutputStream();
			ImageIO.write(saved_image, ext, final_image);
			byte[] imageData = final_image.toByteArray();

			return Response.ok(imageData).build();
		}else {
			String file = AnimalGraph.getImage(uriBase+ id, uriBase+ id + "/images/" + id_img, format);

			if(file.equals("False"))
				return Response.status(404).build();
			else		
				return Response.ok(file).header("Content-Disposition",  "attachment; filename=\""+id+"."+format+"\" ").build();	
		} 	

	}


	@POST
	@Path("{id}/images/{id_img}")
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	public Response postImage(
			@PathParam("id") String id,
			@PathParam("id_img") String id_img,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail) throws Exception {

		String ResourceID = uriBase + id;
		String ResourceImageId = uriBase + id + "/images/" + id_img;
		String path_folder = id;
		String filename = id_img;
		String format = getFileExtension(id_img);

		String uri_exist = AnimalGraph.getImage(ResourceID, ResourceImageId, "rdf");
		if(uri_exist.equals("False")) {
			return Response.status(404).build();
		}
		else {
			AnimalGraph.createImage(uploadedInputStream, path_folder, filename, format);
			String output = "File uploaded to : " + ResourceID;
			return Response.status(200).entity(output).build();
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
		}else if (property.equals("type")) {
			prefix = "http://purl.org/dc/terms/";
		}else if (property.equals("breeder")) {
			prefix = "http://dbpedia.org/ontology/";
		}else if (property.equals("id")){
			prefix = "";
		}else{
			prefix = "http://xmlns.com/foaf/0.1/";
		}
		return prefix;
	}

	private String getFileExtension(String filename) {
		try {
			return filename.substring(filename.lastIndexOf(".") + 1);
		} catch (Exception e) {
			return "";
		}
	}

}
