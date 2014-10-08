package com.theiostream.orcamento;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.syntax.*;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.tdb.TDBFactory;

import java.util.ArrayList;

public class Database {
	protected Dataset dataset;
	
	private String RDF(String s) { return "http://www.w3.org/1999/02/22-rdf-syntax-ns#" + s; }
	private String RDF2(String s) { return "http://www.w3.org/2000/01/rdf-schema#" + s; }
	private String LOA(String s) { return "http://vocab.e.gov.br/2013/09/loa#" + s; }
	
	private Query queryFromBlock(ElementTriplesBlock block) {
		ElementGroup body = new ElementGroup();
		body.addElement(block);

		Query q = QueryFactory.make();
		q.setQueryPattern(body);
		q.setQuerySelectType();

		return q;
	}

	public Database(String year) {
		dataset = TDBFactory.createDataset("/Users/BobNelson/Downloads/apache-jena-2.12.0/tdb");
	}
	

	public ArrayList getOrgans() {
		ArrayList<String> ret = new ArrayList<String>();
		
		ElementTriplesBlock block = new ElementTriplesBlock();
		Var m = Var.alloc("ministerio");
		block.addTriple(Triple.create(m, NodeFactory.createURI(RDF("type")), NodeFactory.createURI(LOA("Orgao"))));
		block.addTriple(Triple.create(m, NodeFactory.createURI(RDF2("label")), Var.alloc("nome")));

		Query q = queryFromBlock(block);
		q.addResultVar("ministerio");
		q.addResultVar("nome");

		QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
		try {
			ResultSet results = qexec.execSelect();
			for (; results.hasNext(); ) {
				QuerySolution soln = results.nextSolution();
				ret.add(soln.getLiteral("nome").getString());
			}
		} finally { qexec.close(); }

		return ret;
	}

	public void executeTest() {
		String sparqlTest = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				  + "PREFIX loa: <http://vocab.e.gov.br/2013/09/loa#>\n"
				  + "SELECT (SUM(?dotacaoInicial) AS ?somaDotacaoInicial) WHERE\n"
				  + "{\n"
				  + "?itemBlankNode loa:temExercicio ?exercicioURI .\n"
				  + "?exercicioURI loa:identificador 2013 .\n"
				  + "?itemBlankNode loa:temGND ?gndURI .\n"
				  + "?gndURI loa:codigo \"1\" .\n"
				  + "?itemBlankNode loa:temUnidadeOrcamentaria ?uoURI .\n"
				  + "?uoURI loa:temOrgao ?orgaoURI .\n"
				  + "?orgaoURI loa:codigo \"26000\" .\n"
				  + "?itemBlankNode loa:valorDotacaoInicial ?dotacaoInicial .\n"
				  + "}";

		Query query = QueryFactory.create(sparqlTest);
		QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
		try {
			ResultSet results = qexec.execSelect();
			for (; results.hasNext(); ) {
				QuerySolution soln = results.nextSolution();
				System.out.println(soln);
			}
		} finally { qexec.close(); }
	}
}
