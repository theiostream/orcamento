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
import static com.theiostream.orcamento.OrcamentoUtils.*;

public class vt {
	public static void main (String[] args) {
		Dataset dataset = TDBFactory.createDataset(args[0]);
		Model model = dataset.getDefaultModel();

		long loa = 0;
		long pago = 0;
		
		Property propertyDotacaoInicial = ResourceFactory.createProperty(LOA("valorDotacaoInicial"));
		Property propertyPago = ResourceFactory.createProperty(LOA("valorPago"));		

		StmtIterator it = model.listStatements(null, ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("ItemDespesa")));
		while (it.hasNext()) {
			Resource r = it.nextStatement().getSubject();
			loa += r.getProperty(propertyDotacaoInicial).getLong();
			pago += r.getProperty(propertyPago).getLong();
		}
		
		System.out.println(loa + "\n" + pago);
	}
}
