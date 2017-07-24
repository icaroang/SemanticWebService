package test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.franz.agraph.jena.AGGraph;
import com.franz.agraph.jena.AGGraphMaker;

import model.PersonGraph;

public class PersonGraphTest {

	@Test
	public void testConnectSingleRepository() throws Exception {
		AGGraph graph = PersonGraph.ConnectSingleRepository();
		assertEquals(graph, graph);		
	}

}
