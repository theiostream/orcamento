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
import java.util.Iterator;

public class Database {
	protected Dataset dataset;
	protected Model model;

	public Database(String year) {
		//dataset = TDBFactory.createDataset(Database.class.getResource("tdb/" + year).getPath());
		dataset = TDBFactory.createDataset("/Users/Daniel/test/orcamento/tdbtest/" + year);
		model = dataset.getDefaultModel();
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

	public String getCodigoForResource(Resource despesa) {
		Statement stmt = model.getProperty(despesa, ResourceFactory.createProperty(LOA("codigo")));
		return stmt.getString();
	}

	public Iterator<OResource> getOResourcesForResource(String rname, Resource orgao) {
		HashMap map = new HashMap<Resource, OResource>();
		
		ResIterator despesas = getDespesasForResource(orgao);
		while (despesas.hasNext()) {
			Resource despesa = despesas.nextResource();
			
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
			}
			else {
				OResource resource = new OResource(r);
				resource.addDespesa(despesa);
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
		Resource subtitle = null;
		
		ResIterator res = model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("codigo")), model.createLiteral(codigo, false));
		while (res.hasNext()) {
			Resource st = res.nextResource();
			
			// We can count on all ItemDespesas of one Subtitulo to have the same Programa and Acao.
			Resource despesa = getDespesasForResource(st).nextResource();

			Resource ac = getPropertyForDespesa(despesa, "Acao");
			Resource pr = getPropertyForDespesa(despesa, "UnidadeOrcamentaria");

			if (programa.equals(pr) && action.equals(ac)) {
				subtitle = st;
			}
		}

		return subtitle;
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
	public double getValorPropertyForDespesa(Resource despesa, String property) {
		Statement stmt = model.getProperty(despesa, ResourceFactory.createProperty(LOA("valor" + property)));
		return stmt.getDouble();
	}

	public HashMap<String, Double> valueForDespesas(ResIterator despesas) {
		HashMap<String, Double> hm = new HashMap<String, Double>();
		
		double dotInicial = 0.0;
		double pago = 0.0;
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
