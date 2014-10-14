package com.theiostream.orcamento;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.syntax.*;

public class OrcamentoUtils {
	public static String RDF(String s) { return "http://www.w3.org/1999/02/22-rdf-syntax-ns#" + s; }
	public static String RDF2(String s) { return "http://www.w3.org/2000/01/rdf-schema#" + s; }
	public static String LOA(String s) { return "http://vocab.e.gov.br/2013/09/loa#" + s; }

	public static Query queryFromBlock(ElementTriplesBlock block) {
                ElementGroup body = new ElementGroup();
                body.addElement(block);

                Query q = QueryFactory.make();
                q.setQueryPattern(body);
                q.setQuerySelectType();

                return q;
        }
}
