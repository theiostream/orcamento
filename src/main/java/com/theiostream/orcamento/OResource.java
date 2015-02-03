package com.theiostream.orcamento;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.ResIteratorImpl;

import java.util.ArrayDeque;

public class OResource {
	protected ArrayDeque<Resource> set;
	protected Resource resource;

	protected double valorLoa = 0;
	protected double valorPago = 0;

	public OResource(Resource res) {
		resource = res;
		set = new ArrayDeque<Resource>();
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

	public void addValorLoa(double v) {
		valorLoa += v;
	}
	public void addValorPago(double v) {
		valorPago += v;
	}

	public double getValorLoa() { return valorLoa; }
	public double getValorPago() { return valorPago; }
	
	@Override
	public boolean equals(Object cmp) {
		return resource.equals(((OResource)cmp).getResource());
	}
	
	@Override
	public int hashCode() {
		return resource.hashCode();
	}
}
