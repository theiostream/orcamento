// Apache Jena <--> Orçamento bridge

package com.theiostream.orcamento;

import static com.theiostream.orcamento.OrcamentoUtils.*;
import com.theiostream.orcamento.OResource;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.ResIteratorImpl;
import com.hp.hpl.jena.rdf.model.impl.StmtIteratorImpl;
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
	
	private static final Property propertyPrevisto;
	private static final Property propertyLancado;
	private static final Property propertyArrecadado;
	private static final Property propertyRecolhido;

	private static final Property propertyProjetoLei;
	private static final Property propertyDotacaoInicial;
	private static final Property propertyLeiMaisCreditos;
	private static final Property propertyEmpenhado;
	private static final Property propertyLiquidado;
	private static final Property propertyPago;

	private static final Property propertyValor;

	private static final Property label;

	private static final HashMap<String, Property> propertyMapper;
	static {
		propertyPrevisto = ResourceFactory.createProperty(LOA("valorPrevisto"));
		propertyLancado = ResourceFactory.createProperty(LOA("valorLancado"));
		propertyArrecadado = ResourceFactory.createProperty(LOA("valorArrecadado"));
		propertyRecolhido = ResourceFactory.createProperty(LOA("valorRecolhido"));

		propertyProjetoLei = ResourceFactory.createProperty(LOA("valorProjetoLei"));
		propertyDotacaoInicial = ResourceFactory.createProperty(LOA("valorDotacaoInicial"));
		propertyLeiMaisCreditos = ResourceFactory.createProperty(LOA("valorLeiMaisCreditos"));
		propertyEmpenhado = ResourceFactory.createProperty(LOA("valorEmpenhado"));
		propertyLiquidado = ResourceFactory.createProperty(LOA("valorLiquidado"));
		propertyPago = ResourceFactory.createProperty(LOA("valorPago"));

		propertyValor = ResourceFactory.createProperty(LOA("valor"));
		
		label = ResourceFactory.createProperty(DC("title"));
		
		propertyMapper = new HashMap<String, Property>();
		propertyMapper.put("Previsto", propertyPrevisto);
		propertyMapper.put("Lancado", propertyLancado);
		propertyMapper.put("Arrecadado", propertyArrecadado);
		propertyMapper.put("Recolhido", propertyRecolhido);

		propertyMapper.put("ProjetoLei", propertyProjetoLei);
		propertyMapper.put("DotacaoInicial", propertyDotacaoInicial);
		propertyMapper.put("LeiMaisCreditos", propertyLeiMaisCreditos);
		propertyMapper.put("Empenhado", propertyEmpenhado);
		propertyMapper.put("Liquidado", propertyLiquidado);
		propertyMapper.put("Pago", propertyPago);
	}
	
	public Database(String year) {
		dataset = TDBFactory.createDataset(year);
		model = dataset.getDefaultModel();

	}
	
	// General
	public String getTypeForResource(Resource resource) {
		String type = model.getProperty(resource, ResourceFactory.createProperty(RDF("type"))).getLiteral().getString();
		String[] s = type.split("/");
		return s[s.length - 1];
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
			else if (type.equals("GND")) {
				if (t.equals("GrupoNatDespesa")) return r;
			}
			else if (t.equals(type)) return r;
		}
		
		return null;
	}

	public String getLabelForResource(Resource r) {
		Statement stmt = r.getProperty(label);
		if (stmt == null) { return null; }
		return stmt.getString();
	}

	public String getCodigoForResource(Resource despesa) {
		Statement stmt = model.getProperty(despesa, ResourceFactory.createProperty(LOA("codigo")));
		return stmt.getString();
	}

	// true if filter success
	public boolean performFilter(Resource despesa, ArrayList<HashMap<String, String> > filter) {
		Iterator it = filter.iterator();
		while (it.hasNext()) {
			HashMap<String, String> map = (HashMap<String, String>)it.next();
			
			String prop = getCodigoForResource(getPropertyForDespesa(despesa, map.get("type")));
			String res = (String)map.get("cod");
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
	public Iterator<OResource> getOResourcesForResource(String rname, Resource orgao, ArrayList<HashMap<String, String> > filter, HashMap<String, ArrayList<String> > xfilter) {
		HashMap map = new HashMap<Resource, OResource>();
		
		ResIterator despesas = getDespesasForResource(orgao);
		while (despesas.hasNext()) {
			Resource despesa = despesas.nextResource();
			if (filter != null) {
				if (!performFilter(despesa, filter)) {
					continue;
				}
			}
			if (xfilter != null) {
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

			OResource resource;
			if (map.containsKey(r)) {
				resource = (OResource)map.get(r);
			}
			else {
				resource = new OResource(r);
				map.put(r, resource);
			}
			
			resource.addDespesa(despesa);
			for (String key : propertyMapper.keySet()) {
				resource.addValor(key, getValorPropertyForDespesa(despesa, key));
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
		else if (typeString.equals("GrupoNatDespesa")) {
			typeString = "GND";
		}

		return model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("tem" + typeString)), resource);
	}
	
	public ResIterator getAll(String type) {
		if (type.equals("GND")) type = "GrupoNatDespesa";
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), model.createLiteral(LOA(type)));		
	}

	// Orgao
	public ResIterator getAllOrgaos() {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(RDF("type")), model.createLiteral(LOA("Orgao")));
	}
	
	public ResIterator getUnidadesForOrgao(Resource orgao) {
		return model.listSubjectsWithProperty(ResourceFactory.createProperty(LOA("temOrgao")), orgao);
	}
	
	public Resource getOrgaoForUnidade(Resource unidade) {
		return model.getProperty(unidade, ResourceFactory.createProperty(LOA("temOrgao"))).getResource();
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

			if (programa.equals(pr) && action.equals(ac) && getTypeForResource(st).equals("Subtitulo")) {
				return st;
			}
		}
		
		//return subtitle;
		return null;
	}

	// ItemDespesa
	public StmtIterator getPropertiesForDespesa(Resource despesa) {
		ArrayDeque<Statement> d = new ArrayDeque<Statement>();		

		StmtIterator properties = despesa.listProperties();
		while (properties.hasNext()) {
			Statement stmt = properties.nextStatement();
			Property property = stmt.getPredicate();
			if (property.getLocalName().startsWith("tem") || property.getLocalName().startsWith("valor"))
				d.add(stmt);
		}

		return new StmtIteratorImpl(d.iterator());
	}

	public Resource getPropertyForDespesa(Resource despesa, String property) {
		if (property.equals("Orgao")) {
			Resource unidade = getPropertyForDespesa(despesa, "UnidadeOrcamentaria");
			return getOrgaoForUnidade(unidade);
		}

		Statement stmt = model.getProperty(despesa, ResourceFactory.createProperty(LOA("tem" + property)));
		return stmt.getResource();
	}
	public long getValorPropertyForDespesa(Resource despesa, String property) {
		Statement stmt = model.getProperty(despesa, propertyMapper.get(property));
		if (stmt == null) {
			stmt = model.getProperty(despesa, propertyValor);
			if (stmt == null) {
				return 0;
			}
		}

		return stmt.getLong();
	}
	
	public HashMap valueForDespesas(ResIterator despesas) {
		return valueForDespesas(despesas, null);
	}

	public HashMap valueForDespesas(ResIterator despesas, ArrayList<HashMap<String, String> > filter) {
		HashMap<String, Long> hm = new HashMap<String, Long>();
		
		while (despesas.hasNext()) {
			Resource r = despesas.nextResource();
			if (filter != null && !performFilter(r, filter)) continue;
			
			for (String key : propertyMapper.keySet()) {
				hm.put(key, (hm.get(key) != null ? hm.get(key) : 0) + getValorPropertyForDespesa(r, key));
			}
		}

		return hm;
	}
}
