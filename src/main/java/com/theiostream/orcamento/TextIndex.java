// TextIndex.java
// Builds Apache Lucene index from Jena RDF database

/* NOTE: There is a jena-text module for jena that does some sort of direct integration with
     Lucene by getting a whole TDB inside a SPARQL-queryable thingy. The author chose not to
     use it because:
     a) Excess of unnecessary information (Subtitulo, ItemDespesas);
     b) Weirdness of configuration;
     c) Is built for use with SPARQL, and not Jena models.
*/

package com.theiostream.orcamento;

import com.theiostream.orcamento.Database;
import com.hp.hpl.jena.rdf.model.*;

import org.apache.lucene.store.Directory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriter;

public class TextIndex {
	public static void build(Directory dir, Database db, Analyzer analyzer) throws java.io.IOException {
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter iwriter = new IndexWriter(dir, config);

		int i=0;
		String[] types = { "Orgao", "UnidadeOrcamentaria", "Funcao", "Subfuncao", "Programa", "Projeto", "OperacaoEspecial", "Atividade" };
		for (String type : types) {
			ResIterator all = db.getAll(type);
			while (all.hasNext()) {
				Document doc = new Document();
				
				Resource r = all.nextResource();
				String cod = db.getCodigoForResource(r);
				String name = db.getLabelForResource(r);
				
				String t = type;
				if (t.equals("Projeto") || t.equals("Atividade") || t.equals("OperacaoEspecial"))
					t = "Acao";

				doc.add(new Field("codigo", cod, TextField.TYPE_STORED));
				doc.add(new Field("type", t, TextField.TYPE_STORED));
				doc.add(new Field("label", name, TextField.TYPE_STORED));
				iwriter.addDocument(doc);

				i++;
			}
		}

		iwriter.close();
		System.out.println("[Orçamento] Created " + i + " items for Lucene index.");
	}
}
