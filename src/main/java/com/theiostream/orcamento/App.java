/* Orçamento Displayer
 * (c) 2014 Daniel Ferreira
 */

package com.theiostream.orcamento;

import static spark.Spark.*;
import com.hp.hpl.jena.rdf.model.*;

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
import java.util.Iterator;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		
		get("/a/:year", (request, response) -> {
			URL str = App.class.getResource("Resource.html");
			URL js = App.class.getResource("All.js");
			return readFile(str) + "\n<script>" + readFile(js) + "</script>";
		});
		get("/a/:year/i", (request, response) -> {
			URL vt = App.class.getResource("vt/" + request.params(":year") + ".txt");
			String f = readFile(vt);
			String[] v = f.split("\n");

			return "{ \"name\": \"Orçamento Federal Brasileiro (" + request.params(":year") + ")\", \"values\": { \"Valor LOA\": " + v[0] + ", \"Valor Pago\": " + v[1] + "} }";
		});
		get("/a/:year/:type", (request, response) -> {
			response.type("application/json");

			Database db = databases.get(request.params(":year"));
			String ret = "{\"name\": \"flare\", \"children\": [";

			long s1 = System.currentTimeMillis();
			ResIterator all = db.getAll(request.params(":type"));
			while (all.hasNext()) {
				Resource r = all.nextResource();
				String name = db.getLabelForResource(r);
				System.out.println("Got name.");
				
				//long s1 = System.currentTimeMillis();
				ResIterator ds = db.getDespesasForResource(r);
				//System.out.println("[" + name + "] getDespesas(): " + (System.currentTimeMillis() - s1));
				//long s2 = System.currentTimeMillis();
				HashMap<String, Long> value = db.valueForDespesas(ds);
				//System.out.println("[" + name + "] valueForDespesas(): " + (System.currentTimeMillis() - s2));
				
				String codigo = db.getCodigoForResource(r);

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] },");
			}
			System.out.println("all() took " + (System.currentTimeMillis() - s1));
			
			ret = ret.substring(0, ret.length()-1);
			return ret.concat("]}");
		});

		get("/r/:year/:type/:org", (request, response) -> {
			URL str = App.class.getResource("Resource.html");
			URL js = App.class.getResource(request.params(":type") + ".js");
			
			if (js == null) return "Not Implemented. Check back later.";

			return readFile(str) + "\n<script>" + readFile(js) + "</script>";
		});
		get("/r/:year/:type/:org/i", (request, response) -> {
			response.type("application/json");
			Database db = databases.get(request.params(":year"));

			String type = request.params(":type");
			Resource res = db.getResourceForCodigo(request.params(":org"), type);
			System.out.println("RES: " + res);
			
			String name = db.getLabelForResource(res);
			System.out.println("LABEL: " + name);

			String parent = null, org = null;
			if (type.equals("UnidadeOrcamentaria")) {
				Resource orgao = db.getOrgaoForUnidade(res);
				parent = db.getLabelForResource(orgao);
				org = "\"Órgão\":\""+parent+"\",";
			}

			HashMap<String, Long> values = db.valueForDespesas(db.getDespesasForResource(res));
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

			System.out.println("{" + r + "}");
			return "{" + r + "}";
		});
		get("/r/:year/:type/:org/:par", (request, response) -> {
			response.type("application/json");
			Database db = databases.get(request.params(":year"));

			String ret = "{\"name\": \"flare\", \"children\": [";
			
			String type = request.params(":type");
			String rtype = request.params(":par");
			Resource orgao = db.getResourceForCodigo(request.params(":org"), type);

			boolean sub = rtype.equals("Subtitulo");
			String p = request.queryParams("p");
			
			HashMap<String, String> filter;
			try {
				String json = request.queryParams("f");
				filter = json == null ? null : new ObjectMapper().readValue(java.net.URLDecoder.decode(json, "UTF-8"), HashMap.class);
			}
			catch (Exception e) {
				return "ERROR BAD BAD";
			}

			long s1 = System.currentTimeMillis();
			Iterator<OResource> functions = db.getOResourcesForResource(rtype, orgao, filter);
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
				
				/*HashMap<String, Double> value;
				value = db.valueForDespesas(function.getDespesas());*/
				
				String codigo = db.getCodigoForResource(function.getResource());

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + function.getValorLoa() + ", \"real\":" + function.getValorPago() + ", \"cod\": \"" + codigo + "\"" + "}] },");
			}
			System.out.println("iter took  " + (System.currentTimeMillis() - s2));
			
			ret = ret.substring(0, ret.length()-1);
			return ret.concat("]}");			
		});
		
		get("/i/:year/:p/:a/:s", (request, response) -> {
			URL str = App.class.getResource("Resource.html");			
			URL js = App.class.getResource("Subtitle.js");

			return readFile(str) + "\n<script>" + readFile(js) + "</script>";
		});
		get("/i/:year/:p/:a/:s/i", (request, response) -> {
			response.type("application/json");
			Database db = databases.get(request.params(":year"));
			
			Resource programa = db.getResourceForCodigo(request.params(":p"), "UnidadeOrcamentaria");
			Resource action = db.getResourceForCodigo(request.params(":a"), "Acao");
			Resource res = db.getSubtitleWithProgramaAndAcao(programa, action, request.params(":s"));

			String name = db.getLabelForResource(res);
			String parent = db.getLabelForResource(action);
			String pname = db.getLabelForResource(programa);

			HashMap<String, Long> values = db.valueForDespesas(db.getDespesasForResource(res));
			return "{ \"name\": \"" + parent + "\", \"parent\": \"" + name + "\", \"programa\": { \"name\": \"" + pname + "\" }, \"values\": { \"Valor LOA\": " + values.get("DotacaoInicial") + ", \"Valor Pago\": " + values.get("Pago") + "}}";

		});
		get("/i/:year/:p/:a/:s/d", (request, response) -> {
			response.type("application/json");
			Database db = databases.get(request.params(":year"));

			Resource programa = db.getResourceForCodigo(request.params(":p"), "UnidadeOrcamentaria");
			Resource action = db.getResourceForCodigo(request.params(":a"), "Acao");
			Resource s = db.getSubtitleWithProgramaAndAcao(programa, action, request.params(":s"));

			String loaArray = "";
			String pagoArray = "";
			
			ResIterator despesas = db.getDespesasForResource(s);
			while (despesas.hasNext()) {
				Resource res = despesas.nextResource();

				System.out.println(res);
				System.out.println(db.getPropertyForDespesa(res, "ElementoDespesa"));

				Resource plano = db.getPropertyForDespesa(res, "PlanoOrcamentario");
				String pl = db.getLabelForResource(plano);

				Resource modalidade = db.getPropertyForDespesa(res, "ModalidadeAplicacao");
				String md = db.getLabelForResource(modalidade);
				
				Resource elemento = db.getPropertyForDespesa(res, "ElementoDespesa");
				String ed = db.getLabelForResource(elemento);

				Resource fonte = db.getPropertyForDespesa(res, "FonteRecursos");
				String fr = db.getLabelForResource(fonte);

				String common = "\"Plano Orçamentário\": \"" + pl + "\", \"Modalidade de Aplicação\": \"" + md + "\", \"Elemento de Despesa\": \"" + ed + "\", \"Fonte de Recursos\": \"" + fr + "\",";
				
				double dot = db.getValorPropertyForDespesa(res, "DotacaoInicial");
				double pago = db.getValorPropertyForDespesa(res, "Pago");
				System.out.println("dot is " + dot + " and pago is " + pago + " and pl is " + db.getValorPropertyForDespesa(res, "ProjetoLei"));
				
				if (dot == 0)
					pagoArray = pagoArray.concat("{" + common + "\"Valor\": " + pago + "},");
				else
					loaArray = loaArray.concat("{" + common + "\"Valor\": " + dot + "},");
			}
			
			pagoArray = pagoArray.substring(0, pagoArray.length()-1);
			loaArray = loaArray.substring(0, loaArray.length()-1);

			return "{ \"Itens Previstos para Cumprimento da LOA\": [" + loaArray + "], \"Itens Pagos (Classificação da SIOP)\": [" + pagoArray + "]}";
		});

		get("/h/:type/:org", (request, response) -> {
			if (request.params(":org").equals("1313")) return "<script>window.location='http://tinyurl.com/o59mblq';</script>";
			URL str = App.class.getResource("Resource.html");
			return readFile(str) + "<script>fillInfo(); createGraphHistory('lol', 'Bozos'); reloadDataHistory('lol', 1);</script>";

		});
		get("/h/:type/:org/i", (request, response) -> {
			response.type("application/json");
			
			HashMap<String, ArrayList> years = new HashMap<String, ArrayList>();

			String type = request.params(":type");
			String latest = "";
			for (HashMap.Entry<String, Database> entry : databases.entrySet()) {
				Database db = entry.getValue();

				Resource res = db.getResourceForCodigo(request.params(":org"), type);
				if (res == null) continue;
				String lbl = db.getLabelForResource(res).trim();
				
				if (years.get(lbl) == null) years.put(lbl, new ArrayList<Integer>());
				years.get(lbl).add(Integer.parseInt(entry.getKey()));

				latest = lbl;
			}
			
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

			return "{ \"name\": \"" + latest + "\", \"parent\": \"Despesas Históricas\", \"values\": " + values + "}";
		});
		get("/h/:type/:org/d", (request, response) -> {
			int rinfo = Integer.parseInt(request.queryParams("rinfo"));
			
			HashMap<String, Double> inflation;
			try {
				String ifile = readFile(App.class.getResource("Inflation.json"));
				inflation = new ObjectMapper().readValue(ifile, HashMap.class);
			}
			catch (Exception e) {
				return "ERROR ERROR BAD";
			}
			
			String ret = "[";

			for (HashMap.Entry<String, Database> entry : databases.entrySet()) {
				Database db = entry.getValue();
				Resource r = db.getResourceForCodigo(request.params(":org"), request.params(":type"));
				if (r == null) { System.out.println("thing doesnt exist at db"); continue; }

				double inf = 1 + (inflation.get(entry.getKey()) / 100);

				HashMap<String, Long> value = db.valueForDespesas(db.getDespesasForResource(r));
				
				switch (rinfo) {
					case 1:
						ret = ret.concat("{ \"letter\": \"" + entry.getKey() + "\", \"loa\": " + value.get("DotacaoInicial") + ", \"pago\": " + value.get("Pago") + ", \"infloa\": " + value.get("DotacaoInicial")*inf + ", \"infpago\": " + value.get("Pago")*inf + "},");
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
				System.out.println("EXCEPTION with query " + request.queryParams("query"));
				return "ERROR ERROR ERROR BAD";
			}
		});

		get("/cs", (request, response) -> {
			URL str = App.class.getResource("Search.html");
			return readFile(str) + "<script>init(); reload();</script>";
		});
		
		/* }}} */
	}
}
