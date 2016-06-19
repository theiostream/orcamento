package com.theiostream.orcamento;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.syntax.*;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.StringBuilder;
import java.net.URL;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import java.lang.Thread;
import java.util.ArrayList;

public class OrcamentoUtils {
	public static String RDF(String s) { return "http://www.w3.org/1999/02/22-rdf-syntax-ns#" + s; }
	public static String RDF2(String s) { return "http://www.w3.org/2000/01/rdf-schema#" + s; }
	public static String LOA(String s) { return "http://www.semanticweb.org/ontologies/OrcamentoPublicoBrasileiro.owl/" + s; }
	public static String DC(String s) { return "http://purl.org/dc/elements/1.1/" + s; }

	public static Query queryFromBlock(ElementTriplesBlock block) {
                ElementGroup body = new ElementGroup();
                body.addElement(block);

                Query q = QueryFactory.make();
                q.setQueryPattern(body);
                q.setQuerySelectType();

                return q;
        }

	public static String readFile(URL str) {
		String ret;

		try (BufferedReader br = new BufferedReader(new FileReader(str.getPath()))) {
			StringBuilder sb = new StringBuilder();

			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}

			ret = sb.toString();
		}
		catch (Exception exception) {
			ret = null;
		}

		return ret;
	}
	
	public static String ystring(ArrayList<Integer> list) {
		if (list.size() == 1) return list.get(0).toString();
		
		int max=0, min=3000;
		for (int i=0; i<list.size(); i++) {
			max = Math.max(max, list.get(i).intValue());
			min = Math.min(min, list.get(i).intValue());
		}
		return min + "-" + max;
	}

	public static TreeMap<String, ArrayList> sortMap(Map map) {
		TreeMap sortedMap = new TreeMap<String, ArrayList<Integer> >(new ValueComparator(map));
		sortedMap.putAll(map);
		return sortedMap;
	}

	public static String ErrorPage() {
		return readFile(OrcamentoUtils.class.getResource("Error.html"));
	}
}

class ValueComparator implements Comparator<String> {
	Map<String, ArrayList<Integer> > base;
	public ValueComparator(Map<String, ArrayList<Integer> > base) {
		this.base = base;
	}

	public int compare(String a, String b) {
		System.out.println(base.get(a).get(0) + " x " + base.get(b).get(0));
		if (base.get(a).get(0) >= base.get(b).get(0))
			return 1;
		return -1;
	}
}

