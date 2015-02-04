/* Orçamento Displayer
 * (c) 2014 Daniel Ferreira
 */

package com.theiostream.orcamento;

import static spark.Spark.*;
import com.theiostream.orcamento.Database;

import java.util.ArrayList;
import com.hp.hpl.jena.rdf.model.*;
import static com.theiostream.orcamento.OrcamentoUtils.*;
import com.theiostream.orcamento.OResource;

import org.codehaus.jackson.map.ObjectMapper;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;

public class App  {
	public static void main(String[] args) {
		staticFileLocation("/com/theiostream/orcamento/static");
		
		HashMap<String, Database> databases = new HashMap<String, Database>(16);
		
		File folder = new File("/Users/Daniel/test/orcamento/tdbtest");
		File[] list = folder.listFiles();
		for (int i=0; i < list.length; i++) {
			System.out.println("File is " + list[i].getPath() + " n " + list[i].getName());
			if (!list[i].isDirectory()) continue;
			
			databases.put(list[i].getName(), new Database(list[i].getPath()));
			System.out.println("Finished " + list[i].getName());
		}

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

			ResIterator all = db.getAll(request.params(":type"));
			while (all.hasNext()) {
				Resource r = all.nextResource();
				String name = r.getProperty(ResourceFactory.createProperty(RDF2("label"))).getString();
				
				//long s1 = System.currentTimeMillis();
				ResIterator ds = db.getDespesasForResource(r);
				//System.out.println("[" + name + "] getDespesas(): " + (System.currentTimeMillis() - s1));
				//long s2 = System.currentTimeMillis();
				HashMap<String, Long> value = db.valueForDespesas(ds);
				//System.out.println("[" + name + "] valueForDespesas(): " + (System.currentTimeMillis() - s2));
				
				String codigo = db.getCodigoForResource(r);

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] },");
			}
			
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
			
			Statement stmt = res.getProperty(ResourceFactory.createProperty(RDF2("label")));
			String name = stmt.getString();

			String parent = "";
			if (type.equals("UnidadeOrcamentaria")) {
				Resource orgao = db.getOrgaoForUnidade(res);
				Statement s = orgao.getProperty(ResourceFactory.createProperty(RDF2("label")));
				parent = s.getString();
			}

			HashMap<String, Long> values = db.valueForDespesas(db.getDespesasForResource(res));
			
			String r = "\"name\": \"" + name + "\", \"parent\": \"" + parent + "\", \"values\": { \"Valor LOA\": " + values.get("DotacaoInicial") + ", \"Valor Pago\": " + values.get("Pago") + "}";
			if (type.equals("Acao")) {
				Iterator<OResource> programas = db.getOResourcesForResource("UnidadeOrcamentaria", res);
				
				if (programas.hasNext()) {
					OResource programa = programas.next();
					if (!programas.hasNext()) {
						String pname = programa.getResource().getProperty(ResourceFactory.createProperty(RDF2("label"))).getString();
						r = r.concat(", \"programa\": { \"name\": \"" + pname + "\", \"cod\": \"" + db.getCodigoForResource(programa.getResource()) + "\" }");
					}
				}
			}

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
				
				Statement stmt = function.getResource().getProperty(ResourceFactory.createProperty(RDF2("label")));
				String name = stmt.getString();
				
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

			Statement stmt = res.getProperty(ResourceFactory.createProperty(RDF2("label")));
			String name = stmt.getString();
			String parent = action.getProperty(ResourceFactory.createProperty(RDF2("label"))).getString();
			String pname = programa.getProperty(ResourceFactory.createProperty(RDF2("label"))).getString();

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

				Resource plano = db.getPropertyForDespesa(res, "PlanoOrcamentario");
				String pl = plano.getProperty(ResourceFactory.createProperty(RDF2("label"))).getString();

				Resource modalidade = db.getPropertyForDespesa(res, "ModalidadeAplicacao");
				String md = modalidade.getProperty(ResourceFactory.createProperty(RDF2("label"))).getString();

				String common = "\"Plano Orçamentário\": \"" + pl + "\", \"Modalidade de Aplicação\": \"" + md + "\", ";
				
				double dot = db.getValorPropertyForDespesa(res, "DotacaoInicial");
				double pago = db.getValorPropertyForDespesa(res, "Pago");

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
			URL str = App.class.getResource("Resource.html");
			return readFile(str) + "<script>fillInfo(); createGraphHistory('lol', 'Bozos'); reloadDataHistory('lol', 1);</script>";

		});
		get("/h/:type/:org/i", (request, response) -> {
			response.type("application/json");

			String type = request.params(":type");
			Resource res = null;
			for (Database db : databases.values()) {
				res = db.getResourceForCodigo(request.params(":org"), type);
				if (res != null) break;
			}
			if (res == null) return "ERROR ERROR BAD";
			
			Statement stmt = res.getProperty(ResourceFactory.createProperty(RDF2("label")));
			String name = stmt.getString();		

			return "{ \"name\": \"" + name + "\", \"parent\": \"Despesas Históricas\" }";
		});
		get("/h/:type/:org/d", (request, response) -> {
			int rinfo = Integer.parseInt(request.queryParams("rinfo"));
			
			HashMap<String, Double> inflation;
			try {
				String ifile = readFile(App.class.getResource("Inflation.json"));
				System.out.println(ifile);
				inflation = new ObjectMapper().readValue(ifile, HashMap.class);
			}
			catch (Exception e) {
				return "ERROR ERROR BAD";
			}
			System.out.println("what " + inflation.get("2000"));
			
			String ret = "[";

			for (HashMap.Entry<String, Database> entry : databases.entrySet()) {
				Database db = entry.getValue();
				Resource r = db.getResourceForCodigo(request.params(":org"), request.params(":type"));
				if (r == null) { System.out.println("null"); continue; }

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

		get("/l/:s", (request, response) -> {
			return null;
		});

		get("/c/:x/:y", (request, response) -> {
			return null;
		});
	}
}
