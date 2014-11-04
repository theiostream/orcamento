package com.theiostream.orcamento;

import static com.theiostream.orcamento.OrcamentoUtils.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.ResIteratorImpl;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.query.*;

import java.util.ArrayDeque;
import java.util.HashMap;

public class Database {
	protected Dataset dataset;
	protected Model model;

	public Database(String year) {
		//dataset = TDBFactory.createDataset(Database.class.getResource("tdb/" + year).getPath());
		dataset = TDBFactory.createDataset("/Users/BobNelson/test/orcamento/tdbtest/" + year);
		model = dataset.getDefaultModel();
	}
	
	// Orgao
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

	// Unidade Orçamentária
	public ResIterator getAllUnidades() {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), ResourceFactory.createResource(LOA("UnidadeOrcamentaria")));
	}

	public ResIterator getDespesasForUnidade(Resource unidade) {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("temUnidadeOrcamentaria")), unidade);
	}

	public Resource getOrgaoForUnidade(Resource unidade) {
		return model.getProperty(unidade, ResourceFactory.createProperty(LOA("temOrgao"))).getResource();
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
