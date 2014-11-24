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
		dataset = TDBFactory.createDataset("/Users/BobNelson/test/orcamento/tdbtest/" + year);
		model = dataset.getDefaultModel();
	}
	
	public String getCodigoForResource(Resource despesa) {
		Statement stmt = model.getProperty(despesa, ResourceFactory.createProperty(LOA("codigo")));
		return stmt.getString();
	}

	// Orgao
	public Resource getOrgaoForCodigo(String cod) {
		ResIterator orgao = model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("codigo")), model.createLiteral(cod, false));
		return orgao.nextResource();
	}

	public ResIterator getAllOrgaos() {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("Orgao")));
	}
	
	public ResIterator getUnidadesForOrgao(Resource orgao) {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("temOrgao")), orgao);
	}

	public ResIterator getDespesasForOrgao(Resource orgao) {
		// This may be less stylish than making a query, but it's way faster.
		ArrayDeque d = new ArrayDeque<Resource>();
		
		ResIterator unidades = getUnidadesForOrgao(orgao);
		while (unidades.hasNext()) {
			Resource unidade = unidades.nextResource();
			ResIterator despesas = getDespesasForUnidade(unidade);
			
			while (despesas.hasNext()) {
				d.add(despesas.nextResource());
			}
		}
		
		return new ResIteratorImpl(d.iterator());
	}
	public ResIterator getFunctionsForOrgao(Resource orgao) {
		HashSet d = new HashSet<Resource>();
		
		ResIterator unidades = getUnidadesForOrgao(orgao);
		while (unidades.hasNext()) {
			Resource unidade = unidades.nextResource();
			ResIterator functions = getFunctionsForUnidade(unidade);

			while (functions.hasNext()) {
				d.add(functions.nextResource());
			}
		}

		return new ResIteratorImpl(d.iterator());
	}

	public ResIterator getDespesasForFunctionInOrgao(Resource function, Resource orgao) {
		ArrayDeque d = new ArrayDeque<Resource>();

		ResIterator unidades = getUnidadesForOrgao(orgao);
		while (unidades.hasNext()) {
			Resource unidade = unidades.nextResource();
			ResIterator despesas = getDespesasForFunctionInUnidade(function, unidade);
			while (despesas.hasNext()) {
				d.add(despesas.nextResource());
			}
		}

		return new ResIteratorImpl(d.iterator());
	}

	// Unidade Orçamentária
	public ResIterator getAllUnidades() {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("UnidadeOrcamentaria")));
	}

	public ResIterator getDespesasForUnidade(Resource unidade) {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("temUnidadeOrcamentaria")), unidade);
	}
	
	/* ****** */
	
	public Iterator<OResource> getResourcesForOrgao(String rname, Resource orgao) {
		HashMap map = new HashMap<Resource, OResource>();
		
		ResIterator despesas = getDespesasForOrgao(orgao);
		while (despesas.hasNext()) {
			Resource despesa = despesas.nextResource();
			
			Resource r = model.getProperty(despesa, ResourceFactory.createProperty(LOA("tem" + rname))).getResource();
			if (map.containsKey(r)) {
				OResource resource = (OResource)map.get(r);
				resource.addDespesa(despesa);
			}
			else {
				OResource resource = new OResource(r);
				map.put(r, resource);
			}
		}

		return map.values().iterator();

	}

	public Iterator<OResource> getResourcesForUnidade(String rname, Resource unidade) {
		HashMap map = new HashMap<Resource, OResource>();
		
		ResIterator despesas = getDespesasForUnidade(unidade);
		while (despesas.hasNext()) {
			Resource despesa = despesas.nextResource();
			
			Resource r = model.getProperty(despesa, ResourceFactory.createProperty(LOA("tem" + rname))).getResource();
			if (map.containsKey(r)) {
				OResource resource = (OResource)map.get(r);
				resource.addDespesa(despesa);
			}
			else {
				OResource resource = new OResource(r);
				map.put(r, resource);
			}
		}

		return map.values().iterator();
	}

	/* ***** */

	public ResIterator getFunctionsForUnidade(Resource unidade) {
		HashSet d = new HashSet<Resource>();
		
		int i = 0;
		ResIterator despesas = getDespesasForUnidade(unidade);
		while (despesas.hasNext()) {
			Resource despesa = despesas.nextResource();
			Resource function = model.getProperty(despesa, ResourceFactory.createProperty(LOA("temFuncao"))).getResource();

			d.add(function);
			i++;
		}

		System.out.println("Functions for Unidade: " + i);
		System.out.println("Count for set: " + d.size());

		return new ResIteratorImpl(d.iterator());
	}

	public ResIterator getDespesasForFunctionInUnidade(Resource function, Resource unidade) {
		ArrayDeque d = new ArrayDeque<Resource>();
		
		ResIterator despesas = getDespesasForUnidade(unidade);
		while (despesas.hasNext()) {
			Resource despesa = despesas.nextResource();
			Resource fn = model.getProperty(despesa, ResourceFactory.createProperty(LOA("temFuncao"))).getResource();
			if (function.equals(fn)) {
				d.add(despesa);
			}
		}

		return new ResIteratorImpl(d.iterator());
	}

	public Resource getOrgaoForUnidade(Resource unidade) {
		return model.getProperty(unidade, ResourceFactory.createProperty(LOA("temOrgao"))).getResource();
	}

	// Functions
	public ResIterator getAllFunctions() {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("Funcao")));
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
		System.out.println(model.getProperty(ResourceFactory.createResource("http://orcamento.dados.gov.br/id/2013/UnidadeOrcamentaria/26255"), ResourceFactory.createProperty(RDF2("label"))).getString());
	}
}
