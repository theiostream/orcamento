/* wr.java
   Remove stupid ItemDespesas from Orçamento TDB shortening exec time by ~25% */

package com.theiostream.orcamento;

import org.apache.jena.atlas.lib.StrUtils;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.update.*;
import com.hp.hpl.jena.query.*;
import java.util.LinkedList;
import java.util.ArrayList;
import static com.theiostream.orcamento.OrcamentoUtils.*;

public class wr {
	public static void main (String[] args) {
		Dataset dataset = TDBFactory.createDataset(args[0]);
		dataset.begin(ReadWrite.WRITE) ;
		
		long s0 = System.currentTimeMillis();
		try {
			Model m = dataset.getDefaultModel();

			// Delete useless ItemDespesas
			GraphStore graphStore = GraphStoreFactory.create(dataset) ;
			String sparqlUpdateString = StrUtils.strjoinNL(
			  "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
			  "PREFIX loa: <http://vocab.e.gov.br/2013/09/loa#>",
			  "PREFIX xml: <http://www.w3.org/2001/XMLSchema#>",
			  "DELETE WHERE {",
			  "?x rdf:type loa:ItemDespesa .",
			  "?x loa:valorDotacaoInicial \"0.00\"^^xml:decimal .",
			  "?x loa:valorProjetoLei \"0.00\"^^xml:decimal .",
			  "?x loa:valorPago \"0.00\"^^xml:decimal .",
			  "?x ?y ?z",
			  "}"
			  ) ;
			UpdateRequest request = UpdateFactory.create(sparqlUpdateString) ;
			UpdateProcessor proc = UpdateExecutionFactory.create(request, graphStore) ;
			
			long s1 = System.currentTimeMillis();
			System.out.println("[wr] Erasing useless ItemDespesas...");
			proc.execute();
			System.out.println("[wr] Erased useless ItemDespesas in " + (System.currentTimeMillis() - s1) + "ms.");
			
			// Make Construct Query
			String construct = StrUtils.strjoinNL(
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
				"PREFIX loa: <http://vocab.e.gov.br/2013/09/loa#>",
				"construct {",
				"	?clown loa:temPlanoOrcamentario ?po ;",
				"		loa:temModalidadeAplicacao ?ma ;",
				"		loa:temIdentificadorUso ?iu ;",
				"		loa:temElementoDespesa ?ed ;",
				"		loa:temFuncao ?fn ;",
				"		loa:temAcao ?ac ;",
				"		loa:temExercicio ?ex ;",
				"		loa:temFonteRecursos ?fr ;",
				"		loa:temCategoriaEconomia ?ce ;",
				"		loa:temEsfera ?es ;",
				"		loa:temSubfuncao ?sf ;",
				"		loa:temSubtitulo ?st ;",
				"		loa:temPrograma ?pr ;",
				"		loa:temUnidadeOrcamentaria ?uo ;",
				"		loa:valorDotacaoInicial ?tdi ;",
				"		loa:valorPago ?tpg ;",
				"		loa:valorProjetoLei ?tpl ;",
				"		rdf:type loa:ItemDespesa",
				"} where {",
				"	{ select (sample(?s) as ?clown) ?po ?ma ?iu ?fn ?ac ?ex ?fr ?ce ?es ?sf ?st ?pr ?ed ?uo (sum(?di) as ?tdi) (sum(?pg) as ?tpg) (sum(?pl) as ?tpl) where {",
				"		?s loa:temPlanoOrcamentario ?po ;",
				"			loa:temModalidadeAplicacao ?ma ;",
				"			loa:temIdentificadorUso ?iu ;",
				"			loa:temElementoDespesa ?ed ;",
				"			loa:temFuncao ?fn ;",
				"			loa:temAcao ?ac ;",
				"			loa:temExercicio ?ex ;",
				"			loa:temFonteRecursos ?fr ;",
				"			loa:temCategoriaEconomica ?ce ;",
				"			loa:temEsfera ?es ;",
				"			loa:temSubfuncao ?sf ;",
				"			loa:temSubtitulo ?st ;",
				"			loa:temPrograma ?pr ;",
				"			loa:temUnidadeOrcamentaria ?uo ;",
				"			loa:valorDotacaoInicial ?di ;",
				"			loa:valorPago ?pg ;",
				"			loa:valorProjetoLei ?pl ;",
				"			rdf:type loa:ItemDespesa",
				"		}",
				"	group by ?po ?ma ?iu ?fn ?ac ?ex ?fr ?ce ?es ?sf ?st ?pr ?uo ?ed }",
				"}");
			
			// Copy Model
			long s2 = System.currentTimeMillis();
			System.out.println("[wr] Copying model...");
			Model mm = ModelFactory.createDefaultModel();
			mm.add(m);
			System.out.println("[wr] Copied model in " + (System.currentTimeMillis() - s2) + "ms.");

			// Remove all ItemDespesas from model (takes way too long)
			long s4 = System.currentTimeMillis();
			System.out.println("[wr] Temporarily deleting all ItemDespesas...");
			
			StmtIterator it = m.listStatements(null, ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("ItemDespesa")));
			LinkedList l = new LinkedList<Statement>();
			while (it.hasNext()) {
				Statement n = it.nextStatement();
				StmtIterator it2 = m.listStatements(n.getSubject(), null, (RDFNode)null);
				while (it2.hasNext()) l.add(it2.nextStatement());
			}
			m.remove(l);
			System.out.println("[wr] Erased all ItemDespesas in " + (System.currentTimeMillis() - s4) + "ms.");
			
			// Execute Construct query
			Query q = QueryFactory.create(construct);
			QueryExecution qexec = QueryExecutionFactory.create(q, mm);
			
			long s3 = System.currentTimeMillis();
			System.out.println("[wr] Merging items with same properties...");
			qexec.execConstruct(m);
			System.out.println("[wr] Merged in " + (System.currentTimeMillis() - s3) + "ms.");

			System.out.println("[wr] Committing changes to TDB...");
			dataset.commit();
		}
		finally { 
			dataset.end();
		}

		System.out.println("[wr] Finished. Execution time: " + (System.currentTimeMillis() - s0) + "ms.");
		System.exit(0);
	}
}
