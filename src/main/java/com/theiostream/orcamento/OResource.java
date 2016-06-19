package com.theiostream.orcamento;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.ResIteratorImpl;

import java.util.ArrayDeque;
import java.util.HashMap;

public class OResource {
	protected ArrayDeque<Resource> set;
	protected Resource resource;
	
	protected HashMap<String, Long> values;

	public OResource(Resource res) {
		resource = res;
		set = new ArrayDeque<Resource>();
		values = new HashMap<String, Long>();
	}

	public Resource getResource() {
		return resource;
	}

	public void addDespesa(Resource despesa) {
		set.add(despesa);
	}

	public ResIterator getDespesas() {
		return new ResIteratorImpl(set.iterator());
	}

	public void addValor(String property, long v) {
		values.put(property, (values.get(property) != null ? values.get(property) : 0) + v);
	}

	public long getValor(String property) { return values.get(property); }

	public HashMap getValores() { return values; }
	
	@Override
	public boolean equals(Object cmp) {
		return resource.equals(((OResource)cmp).getResource());
	}
	
	@Override
	public int hashCode() {
		return resource.hashCode();
	}
}
