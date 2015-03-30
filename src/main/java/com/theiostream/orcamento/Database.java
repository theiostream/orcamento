// Database.java
// Apache Jena <--> Orçamento bridge

package com.theiostream.orcamento;

import static com.theiostream.orcamento.OrcamentoUtils.*;
import com.theiostream.orcamento.OResource;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.ResIteratorImpl;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.query.*;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;

public class Database {
	protected Dataset dataset;
	protected Model model;
	
	protected Property propertyDotacaoInicial;
	protected Property propertyPago;
	protected Property label;

	public Database(String year) {
		//dataset = TDBFactory.createDataset(Database.class.getResource("tdb/" + year).getPath());
		//dataset = TDBFactory.createDataset("/Users/Daniel/test/orcamento/tdbtest/" + year);
		dataset = TDBFactory.createDataset(year);
		model = dataset.getDefaultModel();

		propertyDotacaoInicial = ResourceFactory.createProperty(LOA("valorDotacaoInicial"));
		propertyPago = ResourceFactory.createProperty(LOA("valorPago"));
		label = ResourceFactory.createProperty(RDF2("label"));
	}
	
	// General
	public String getTypeForResource(Resource resource) {
		Resource type = model.getProperty(resource, ResourceFactory.createProperty(RDF("type"))).getResource();
		String[] s = type.getURI().split("#");
		return s[1];	
	}

	public Resource getResourceForCodigo(String cod, String type) {
		ResIterator res = model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("codigo")), model.createLiteral(cod, false));
		while (res.hasNext()) {
			Resource r = res.nextResource();
			String t = getTypeForResource(r);

			if (type.equals("Acao")) {
				if (t.equals(type) || t.equals("OperacaoEspecial") || t.equals("Projeto") || t.equals("Atividade")) {
					return r;
				}
			}
			else if (t.equals(type)) return r;
		}
		
		return null;
	}

	public String getLabelForResource(Resource r) {
		Statement stmt = r.getProperty(label);
		return stmt.getString();
	}

	public String getCodigoForResource(Resource despesa) {
		Statement stmt = model.getProperty(despesa, ResourceFactory.createProperty(LOA("codigo")));
		return stmt.getString();
	}

	// true if filter success
	public boolean performFilter(Resource despesa, HashMap<String, String> filter) {
		Iterator it = filter.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			
			String prop = getCodigoForResource(getPropertyForDespesa(despesa, (String)pair.getKey()));
			String res = (String)pair.getValue();
			if (!prop.equals(res)) return false;
		}

		return true;
	}
	
	// true is exclusion success
	public boolean performExclusionFilter(Resource despesa, HashMap<String, ArrayList<String> > xfilter) {
		Iterator it = xfilter.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();

			String resource = getCodigoForResource(getPropertyForDespesa(despesa, (String)pair.getKey()));
			for (String s : ((ArrayList<String>)pair.getValue())) {
				if (resource.equals(s)) return false;
			}
		}

		return true;
	}

	public Iterator<OResource> getOResourcesForResource(String rname, Resource orgao) {
		return getOResourcesForResource(rname, orgao, null, null);
	}
	public Iterator<OResource> getOResourcesForResource(String rname, Resource orgao, HashMap<String, String> filter, HashMap<String, ArrayList<String> > xfilter) {
		HashMap map = new HashMap<Resource, OResource>();
		
		ResIterator despesas = getDespesasForResource(orgao);
		while (despesas.hasNext()) {
			Resource despesa = despesas.nextResource();
			if (filter != null) {
				if (!performFilter(despesa, filter)) {
					continue;
				}
			}
			else if (xfilter != null) {
				if (!performExclusionFilter(despesa, xfilter)) {
					continue;
				}
			}
			
			Resource r;
			if (rname.equals("Orgao")) {
				Resource u = model.getProperty(despesa, ResourceFactory.createProperty(LOA("temUnidadeOrcamentaria"))).getResource();
				r = model.getProperty(u, ResourceFactory.createProperty(LOA("temOrgao"))).getResource();
			}
			else
				r = model.getProperty(despesa, ResourceFactory.createProperty(LOA("tem" + rname))).getResource();
			
			if (map.containsKey(r)) {
				OResource resource = (OResource)map.get(r);
				resource.addDespesa(despesa);
				resource.addValorLoa(getValorPropertyForDespesa(despesa, "DotacaoInicial"));
				resource.addValorPago(getValorPropertyForDespesa(despesa, "Pago"));
			}
			else {
				OResource resource = new OResource(r);
				resource.addDespesa(despesa);
				resource.addValorLoa(getValorPropertyForDespesa(despesa, "DotacaoInicial"));
				resource.addValorPago(getValorPropertyForDespesa(despesa, "Pago"));
				
				map.put(r, resource);
			}
		}

		return map.values().iterator();
	}	

	public ResIterator getDespesasForResource(Resource resource) {
		String typeString = getTypeForResource(resource);
		
		if (typeString.equals("Orgao")) {
			ArrayDeque<Resource> d = new ArrayDeque<Resource>();

			ResIterator unidades = getUnidadesForOrgao(resource);
			while (unidades.hasNext()) {
				Resource unidade = unidades.nextResource();
				ResIterator despesas = getDespesasForResource(unidade);
				
				while (despesas.hasNext())
					d.add(despesas.nextResource());
			}

			return new ResIteratorImpl(d.iterator());
		}

		if (typeString.equals("Atividade") || typeString.equals("Projeto") || typeString.equals("OperacaoEspecial")) {
			typeString = "Acao";
		}

		return model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("tem" + typeString)), resource);
	}
	
	public ResIterator getAll(String type) {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA(type)));		
	}

	// Orgao
	public ResIterator getAllOrgaos() {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("Orgao")));
	}
	
	public ResIterator getUnidadesForOrgao(Resource orgao) {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("temOrgao")), orgao);
	}

	// Unidade Orçamentária
	public ResIterator getAllUnidades() {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("UnidadeOrcamentaria")));
	}

	public Resource getOrgaoForUnidade(Resource unidade) {
		return model.getProperty(unidade, ResourceFactory.createProperty(LOA("temOrgao"))).getResource();
	}

	// Functions
	public ResIterator getAllFunctions() {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("Funcao")));
	}

	// Subtitle
	public Resource getSubtitleWithProgramaAndAcao(Resource programa, Resource action, String codigo) {
		//Resource subtitle = null;
		
		ResIterator res = model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("codigo")), model.createLiteral(codigo, false));
		while (res.hasNext()) {
			Resource st = res.nextResource();

			// We can count on all ItemDespesas of one Subtitulo to have the same Programa and Acao.
			ResIterator d = getDespesasForResource(st);
			if (!d.hasNext()) { continue; }

			Resource despesa = d.nextResource();

			Resource ac = getPropertyForDespesa(despesa, "Acao");
			Resource pr = getPropertyForDespesa(despesa, "UnidadeOrcamentaria");

			if (programa.equals(pr) && action.equals(ac)) {
				/*System.out.println("$$ EQ!!!!!");
				subtitle = st;*/
				return st;
			}
		}
		
		//return subtitle;
		return null;
	}

	// ItemDespesa
	public Resource getPropertyForDespesa(Resource despesa, String property) {
		if (property.equals("Orgao")) {
			Resource unidade = getPropertyForDespesa(despesa, "UnidadeOrcamentaria");
			return getOrgaoForUnidade(unidade);
		}

		Statement stmt = model.getProperty(despesa, ResourceFactory.createProperty(LOA("tem" + property)));
		return stmt.getResource();
	}
	public long getValorPropertyForDespesa(Resource despesa, String property) {
		Statement stmt = model.getProperty(despesa, property.equals("DotacaoInicial") ? propertyDotacaoInicial : propertyPago);
		return stmt.getLong();
	}

	public HashMap<String, Long> valueForDespesas(ResIterator despesas) {
		HashMap<String, Long> hm = new HashMap<String, Long>();
		
		long dotInicial = 0;
		long pago = 0;
		while (despesas.hasNext()) {
			Resource r = despesas.nextResource();
			dotInicial += getValorPropertyForDespesa(r, "DotacaoInicial");
			pago += getValorPropertyForDespesa(r, "Pago");
		}
		
		hm.put("DotacaoInicial", dotInicial);
		hm.put("Pago", pago);

		return hm;
	}

	public void executeTest() {
	}
}
