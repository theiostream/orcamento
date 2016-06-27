/* Orçamento Displayer
 * (c) 2014 Daniel Ferreira
 */

package com.theiostream.orcamento;

import static spark.Spark.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.ResIteratorImpl;


import com.theiostream.orcamento.Database;
import com.theiostream.orcamento.TextIndex;
import com.theiostream.orcamento.OResource;
import static com.theiostream.orcamento.OrcamentoUtils.*;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;

import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;
import java.net.URL;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayDeque;

class IntegerComparator implements Comparator<Integer>{
	public int compare(Integer o1, Integer o2) {
		return (o1<o2 ? -1 : (o1==o2 ? 0 : 1));
	}
}

public class App {
	public static void main(String[] args) {
		staticFileLocation("/com/theiostream/orcamento/static");
		
		/* Initialization {{{ */

		System.out.println("[Orçamento] Initializing Databases...");
		HashMap<String, Database> databases = new HashMap<String, Database>(16);
		
		File folder = new File("/Users/Daniel/test/orcamento/tdbtest");
		File[] list = folder.listFiles();
		for (int i=0; i < list.length; i++) {
			if (!list[i].isDirectory()) continue;
			
			databases.put(list[i].getName(), new Database(list[i].getPath()));
		}
		System.out.println("[Orçamento] Initialized Databases.");

		System.out.println("[Orçamento] Initializing Lucene Stores...");
		HashMap<String, Directory> directories = new HashMap<String, Directory>(16);

		for (HashMap.Entry<String, Database> entry : databases.entrySet()) {
			try {
				Path p = Paths.get("/Users/Daniel/test/orcamento/lucenetest/" + entry.getKey());
				
				BrazilianAnalyzer analyzer = new BrazilianAnalyzer();
				Directory dir = FSDirectory.open(p);
				if (!DirectoryReader.indexExists(dir)) {
					TextIndex.build(dir, entry.getValue(), analyzer);
				}
			}
			catch (Exception e) {
				System.out.println("[Orçamento] Lucene Store initialization threw exception " + e);
			}
		}

		System.out.println("[Orçamento] Initialized Lucene Stores.");

		/* }}} */

		/* Website {{{ */
		get("/", (request, response) -> {
			return readFile(App.class.getResource("index.html"));
		});
		
		get("/a/:set/:year", (request, response) -> {
			Database db = databases.get(request.params(":set") + "_" + request.params(":year"));
			if (db == null) return ErrorPage();
			
			URL str = App.class.getResource("Resource.html");
			URL common = App.class.getResource(request.params(":set") + "/Common.js");
			URL js = App.class.getResource(request.params(":set") + "/All.js");
			return readFile(str) + "\n<script>" + readFile(common) + ";" + readFile(js) + "</script>";
		});
		get("/a/:set/:year/i", (request, response) -> {
			URL vt = App.class.getResource("vt/" + request.params(":set") + "/" + request.params(":year") + ".txt");
			String f = readFile(vt);
			String[] v = f.split("\n");

			return "{ \"name\": \"Orçamento Federal (" + request.params(":year") + ")\", \"values\": { \"Valor LOA\": " + v[0] + ", \"Valor Pago\": " + v[1] + "} }";
		});
		get("/a/:set/:year/:type", (request, response) -> {
			response.type("application/json");
			
			HashMap<String, ArrayList<String> > xfilter = null;
			if (request.queryParams("xf") != null) {
				try {
					String xjson = request.queryParams("xf");
					xfilter = xjson == null ? null : new ObjectMapper().readValue(java.net.URLDecoder.decode(xjson, "UTF-8"), HashMap.class);
				}
				catch (Exception e) {
					xfilter = null;
				}
			}

			Database db = databases.get(request.params(":set") + "_" + request.params(":year"));
			String ret = "{\"name\": \"flare\", \"children\": [";

			long s1 = System.currentTimeMillis();
			ResIterator all = db.getAll(request.params(":type"));
			while (all.hasNext()) {
				Resource r = all.nextResource();
				
				String name = db.getLabelForResource(r);
				String codigo = db.getCodigoForResource(r);
				ResIterator ds = db.getDespesasForResource(r);

				if (xfilter != null) {
					ArrayDeque<Resource> filtered = new ArrayDeque<Resource>();
					while (ds.hasNext()) {
						Resource despesa = ds.nextResource();
						if (!db.performExclusionFilter(despesa, xfilter)) continue;
						filtered.add(despesa);
					}
					ds = new ResIteratorImpl(filtered.iterator());
				}
				
				HashMap<String, Object> value = db.valueForDespesas(ds);
				value.put("name", name);
				value.put("cod", codigo);				
				
				String valuestring;
				try {
					valuestring = new ObjectMapper().writeValueAsString(value);
				}
				catch (Exception e) {
					valuestring = "";
				}


				ret = ret.concat(valuestring + ",");
			}
			System.out.println("all() took " + (System.currentTimeMillis() - s1));
			
			ret = ret.substring(0, ret.length()-1);
			return ret.concat("]}");
		});

		get("/r/:set/:year/:type/:org", (request, response) -> {
			Database db = databases.get(request.params(":set") + "_" + request.params(":year"));
			if (db == null) return ErrorPage();
			String type = request.params(":type");
			Resource res = db.getResourceForCodigo(request.params(":org"), type);
			if (res == null) return ErrorPage();
			
			URL str = App.class.getResource("Resource.html");
			URL common = App.class.getResource(request.params(":set") + "/Common.js");
			URL js = App.class.getResource(request.params(":set") + "/" + request.params(":type") + ".js");

			return readFile(str) + "\n<script>" + readFile(common) + ";" + readFile(js) + "</script>";
		});
		get("/r/:set/:year/:type/:org/i", (request, response) -> {
			response.type("application/json");
			Database db = databases.get(request.params(":set") + "_" + request.params(":year"));

			String type = request.params(":type");
			Resource res = db.getResourceForCodigo(request.params(":org"), type);
			
			String name = db.getLabelForResource(res);

			String parent = null, org = null;
			/*if (type.equals("UnidadeOrcamentaria")) {
				Resource orgao = db.getOrgaoForUnidade(res);
				parent = db.getLabelForResource(orgao);
				org = "\"Órgão\":\""+parent+"\",";
			}*/

			ResIterator despesas = db.getDespesasForResource(res);
			
			ArrayList<HashMap<String, String> > filter;
			try {
				String json = request.queryParams("f");
				filter = json == null ? null : new ObjectMapper().readValue(java.net.URLDecoder.decode(json, "UTF-8"), ArrayList.class);
			}
			catch (Exception e) {
				filter = null;
			}

			HashMap<String, Long> values = db.valueForDespesas(despesas, filter);
			String r = "\"name\": \"" + name + "\", \"values\": { " + (parent!=null?org:"") + "\"Valor LOA\": " + values.get("DotacaoInicial") + ", \"Valor Pago\": " + values.get("Pago") + "}";
			if (type.equals("Acao")) {
				Iterator<OResource> programas = db.getOResourcesForResource("UnidadeOrcamentaria", res);
				
				if (programas.hasNext()) {
					OResource programa = programas.next();
					if (!programas.hasNext()) {
						String pname = db.getLabelForResource(programa.getResource());
						r = r.concat(", \"programa\": { \"name\": \"" + pname + "\", \"cod\": \"" + db.getCodigoForResource(programa.getResource()) + "\" }");
					}
				}
			}

			return "{" + r + "}";
		});
		get("/r/:set/:year/:type/:org/:par", (request, response) -> {
			response.type("application/json");
			Database db = databases.get(request.params(":set") + "_" + request.params(":year"));

			String ret = "{\"name\": \"flare\", \"children\": [";
			
			String type = request.params(":type");
			String rtype = request.params(":par");
			Resource orgao = db.getResourceForCodigo(request.params(":org"), type);

			boolean sub = rtype.equals("Subtitulo");
			String p = request.queryParams("p");
			
			ArrayList<HashMap<String, String> > filter;
			HashMap<String, ArrayList<String> > xfilter;
			try {
				String json = request.queryParams("f");
				filter = json == null ? null : new ObjectMapper().readValue(java.net.URLDecoder.decode(json, "UTF-8"), ArrayList.class);

				String xjson = request.queryParams("xf");
				xfilter = xjson == null ? null : new ObjectMapper().readValue(java.net.URLDecoder.decode(xjson, "UTF-8"), HashMap.class);
			}
			catch (Exception e) {
				filter = null;
				xfilter = null;
			}

			long s1 = System.currentTimeMillis();
			Iterator<OResource> functions = db.getOResourcesForResource(rtype, orgao, filter, xfilter);
			System.out.println("getORes took " + (System.currentTimeMillis() - s1));
			
			long s2 = System.currentTimeMillis();
			while (functions.hasNext()) {
				OResource function = functions.next();
				if (sub == true && p != null) {
					Resource despesa = db.getDespesasForResource(function.getResource()).nextResource();

					Resource programa = db.getPropertyForDespesa(despesa, "UnidadeOrcamentaria");

					String c = db.getCodigoForResource(programa);
					if (!c.equals(p)) continue;
				}
				
				String name = db.getLabelForResource(function.getResource());
				String codigo = db.getCodigoForResource(function.getResource());

				HashMap<String, Object> value = function.getValores();
				value.put("name", name);
				value.put("cod", codigo);
				
				String valuestring;
				try {
					valuestring = new ObjectMapper().writeValueAsString(value);
				}
				catch (Exception e) {
					valuestring = "";
				}				

				ret = ret.concat(valuestring + ",");
			}
			System.out.println("iter took  " + (System.currentTimeMillis() - s2));
			
			ret = ret.substring(0, ret.length()-1);
			return ret.concat("]}");			
		});
		
		get("/i/:set/:year/:type/:cod", (request, response) -> {
			Database db = databases.get(request.params(":set") + "_" + request.params(":year"));
			if (db == null) return ErrorPage();
			
			String cod = request.params(":cod");
			Resource res;

			if (request.params(":type").equals("Subtitulo")) {
				String p = cod.substring(0, 4);
				String a = cod.substring(5, 8);
				String s = cod.substring(9, 12);
				
				Resource programa = db.getResourceForCodigo(p, "UnidadeOrcamentaria");
				Resource action = db.getResourceForCodigo(a, "Acao");
				if (programa == null || action == null) return ErrorPage();

				res = db.getSubtitleWithProgramaAndAcao(programa, action, s);
			}
			else
				res = db.getResourceForCodigo(cod, request.params(":type"));

			if (res == null) return ErrorPage();

			URL str = App.class.getResource("Resource.html");
			URL common = App.class.getResource(request.params(":set") + "/Common.js");			
			URL js = App.class.getResource(request.params(":set") + "/Table-" + request.params(":type") + ".js");

			return readFile(str) + "\n<script>" + readFile(common) + ";" + readFile(js) + "</script>";
		});
		get("/i/:set/:year/:type/:cod/i", (request, response) -> {
			response.type("application/json");
			Database db = databases.get(request.params(":set") + "_" + request.params(":year"));
			
			String cod = request.params(":cod");

			if (request.params(":type").equals("Subtitulo")) {
				String p = cod.substring(0, 4);
				String a = cod.substring(5, 8);
				String s = cod.substring(9, 12);
				
				Resource programa = db.getResourceForCodigo(p, "UnidadeOrcamentaria");
				Resource action = db.getResourceForCodigo(a, "Acao");
				if (programa == null || action == null) return ErrorPage();

				Resource res = db.getSubtitleWithProgramaAndAcao(programa, action, s);
				
				String name = db.getLabelForResource(res);
				String parent = db.getLabelForResource(action);
				String pname = db.getLabelForResource(programa);

				HashMap<String, Long> values = db.valueForDespesas(db.getDespesasForResource(res));
				return "{ \"name\": \"" + parent + "\", \"parent\": \"" + name + "\", \"programa\": { \"name\": \"" + pname + "\" }, \"values\": { \"Valor LOA\": " + values.get("DotacaoInicial") + ", \"Valor Pago\": " + values.get("Pago") + "}}";
			}
			
			Resource res = db.getResourceForCodigo(cod, request.params(":type"));
			String name = db.getLabelForResource(res);
			HashMap<String, Long> values = db.valueForDespesas(db.getDespesasForResource(res));
			
			return "{ \"name\": \"" + name + "\", \"values\": { \"Valor LOA\": " + values.get("DotacaoInicial") + ", \"Valor Pago\": " + values.get("Pago") + "} }";

		});
		get("/i/:set/:year/:type/:cod/d", (request, response) -> {
			/* TODO: There is an implementation dilemma here.
			 * At the SOF ontology, ItemDespesas have one value and all others are nil. Therefore,
			 * it makes sense for all paid and all LOA to be aggregated.
			 * However, in the USP ontologies values should *ideally* be in the same ItemDespesa.
			 * In this branch, this logic has been changed to list all items without aggregating. */
			
			response.type("application/json");
			Database db = databases.get(request.params(":set") + "_" + request.params(":year"));

			String cod = request.params(":cod");
			Resource res;

			if (request.params(":type").equals("Subtitulo")) {
				String p = cod.substring(0, 4);
				String a = cod.substring(5, 8);
				String s = cod.substring(9, 12);
				
				Resource programa = db.getResourceForCodigo(p, "UnidadeOrcamentaria");
				Resource action = db.getResourceForCodigo(a, "Acao");
				if (programa == null || action == null) return ErrorPage();

				res = db.getSubtitleWithProgramaAndAcao(programa, action, s);
			}
			else
				res = db.getResourceForCodigo(cod, request.params(":type"));			
			
			String ret = "[";

			ResIterator despesas = db.getDespesasForResource(res);
			while (despesas.hasNext()) {
				ret = ret.concat("{");
				Resource despesa = despesas.nextResource();

				StmtIterator properties = db.getPropertiesForDespesa(despesa);
				while (properties.hasNext()) {
					Statement stmt = properties.nextStatement();
					Property property = stmt.getPredicate();
					
					boolean valor = property.getLocalName().startsWith("valor");
					Object s = valor ? Long.valueOf(stmt.getLong()) : db.getLabelForResource(stmt.getResource());
					
					String pname = valor ? property.getLocalName().substring(5) : property.getLocalName().substring(3);
					
					// FIXME TEMPORARY: This is necessary until the ontology gives up on a "valor".
					// This is terrible.
					if (valor && pname.equals("")) pname = "Generico";

					ret = ret.concat("\"" + pname + "\": \"" + s + "\",");
				}
				
				ret = ret.substring(0, ret.length()-1);
				ret = ret.concat("},");
			}
			
			ret = ret.substring(0, ret.length()-1);
			return ret.concat("]");
		});

		get("/h/:set/:type/:org", (request, response) -> {
			int c = 0;
			for (HashMap.Entry<String, Database> entry : databases.entrySet()) {
				Database db = entry.getValue();
				Resource r = db.getResourceForCodigo(request.params(":org"), request.params(":type"));
				if (r == null) { continue; }
				c++;
			}
			if (c == 0) return ErrorPage();

			URL str = App.class.getResource("Resource.html");
			return readFile(str) + "<script>fillInfo(); createGraphHistory('lol', 'Bozos'); reloadDataHistory('lol', 1);</script>";

		});
		get("/h/:set/:type/:org/i", (request, response) -> {
			response.type("application/json");
			
			TreeMap<String, ArrayList> years = new TreeMap<String, ArrayList>();

			String type = request.params(":type");
			for (HashMap.Entry<String, Database> entry : databases.entrySet()) {
				Database db = entry.getValue();

				Resource res = db.getResourceForCodigo(request.params(":org"), type);
				if (res == null) continue;
				String lbl = db.getLabelForResource(res).trim();
				
				if (years.get(lbl) == null) years.put(lbl, new ArrayList<Integer>());
				years.get(lbl).add(Integer.parseInt(entry.getKey()));
			}
			for (ArrayList v : years.values()) {
				Collections.sort(v, new IntegerComparator());
			}

			years = sortMap(years);
			
			String values;
			if (years.size() > 1) {
				values= "{";
				for (HashMap.Entry<String, ArrayList> y : years.entrySet()) {
					values += "\"" + y.getKey() + "\":\"" + ystring(y.getValue()) + "\",";
				}
				values = values.substring(0, values.length()-1);
				values += "}";
			}
			else values = "{}";

			return "{ \"name\": \"" + years.lastEntry().getKey() + "\", \"parent\": \"Despesas Históricas\", \"values\": " + values + "}";
		});
		get("/h/:set/:type/:org/d", (request, response) -> {
			response.type("application/json");			
			
			int rinfo = Integer.parseInt(request.queryParams("rinfo"));
			
			HashMap<String, Double> inflation;
			try {
				String ifile = readFile(App.class.getResource("Inflation.json"));
				inflation = new ObjectMapper().readValue(ifile, HashMap.class);
			}
			catch (Exception e) {
				inflation = null;
			}
			
			String ret = "[";

			for (HashMap.Entry<String, Database> entry : databases.entrySet()) {
				String[] info = entry.getKey().split("_");
				String dataset = info[0];
				if (!dataset.equals(request.params(":set"))) continue;
				String year = info[1];
				
				Database db = entry.getValue();
				Resource r = db.getResourceForCodigo(request.params(":org"), request.params(":type"));
				if (r == null) { continue; }

				double inf;
				double i = inflation.get(year);
				if (inflation != null) inf = 1 + (i / 100);
				else inf = 0;

				HashMap<String, Long> value = db.valueForDespesas(db.getDespesasForResource(r));
				
				switch (rinfo) {
					case 1:
						ret = ret.concat("{ \"letter\": \"" + year + "\", \"loa\": " + value.get("DotacaoInicial") + ", \"pago\": " + value.get("Pago") + ", \"infloa\": " + value.get("DotacaoInicial")*inf + ", \"infpago\": " + value.get("Pago")*inf + "},");
						break;
					default:
						break;
				}
			}
			
			ret = ret.substring(0, ret.length()-1);
			return ret.concat("]");
		});

		get("/c", (request, response) -> {
			URL str = App.class.getResource("Compare.html");
			return readFile(str) + "<script>init();</script>";
		});
		post("/cp", (request, response) -> {
			Database db1 = databases.get(request.queryParams("y1"));
			Database db2 = databases.get(request.queryParams("y2"));

			Resource r1 = db1.getResourceForCodigo(request.queryParams("c1"), request.queryParams("t1"));
			String l1 = db1.getLabelForResource(r1);
			Resource r2 = db2.getResourceForCodigo(request.queryParams("c2"), request.queryParams("t2"));
			String l2 = db2.getLabelForResource(r2);

			HashMap<String, Long> v1 = db1.valueForDespesas(db1.getDespesasForResource(r1));
			HashMap<String, Long> v2 = db2.valueForDespesas(db2.getDespesasForResource(r2));

			String vt1 = request.queryParams("v1").equals("LOA") ? "DotacaoInicial" : "Pago";
			String vt2 = request.queryParams("v2").equals("LOA") ? "DotacaoInicial" : "Pago";

			return "[{\"c\":0,\"res\":\""+l1+"\", \"value\":"+v1.get(vt1)+"}, {\"c\":1,\"res\":\"" + l2 +"\", \"value\":"+v2.get(vt2)+"}]";
		});

		post("/s", (request, response) -> {
			try {
				String ret = "[";

				BrazilianAnalyzer analyzer = new BrazilianAnalyzer();
				Path p = Paths.get("/Users/Daniel/test/orcamento/lucenetest/" + request.queryParams("year"));
				Directory dir = FSDirectory.open(p);

				DirectoryReader reader = DirectoryReader.open(dir);
				IndexSearcher searcher = new IndexSearcher(reader);

				QueryParser parser = new QueryParser("label", analyzer);
				Query query = parser.parse(java.net.URLDecoder.decode(request.queryParams("query"), "UTF-8"));

				ScoreDoc[] hits = searcher.search(query, null, Integer.parseInt(request.queryParams("count"))).scoreDocs;
				for (int i=0; i<hits.length; i++) {
					Document hitDoc = searcher.doc(hits[i].doc);
					ret = ret.concat("{ \"value\": \"" + hitDoc.get("label") + "\", \"type\": \"" + hitDoc.get("type") + "\", \"codigo\": \"" + hitDoc.get("codigo") + "\" },");
				}

				if (ret.length() > 1) ret = ret.substring(0, ret.length()-1);
				return ret.concat("]");
			}
			catch (Exception e) {
				return "[]";
			}
		});

		get("/cs", (request, response) -> {
			URL str = App.class.getResource("Search.html");
			return readFile(str) + "<script>init(); reload();</script>";
		});
		
		/* }}} */
	}
}
