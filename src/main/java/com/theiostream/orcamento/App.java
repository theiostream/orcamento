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

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

public class App  {
	public static void main(String[] args) {
		Database db = new Database("2013");
		
		/*get("/static/:file", (request, response) -> {
			URL str = App.class.getResource("static/" + request.params(":file"));
			return readFile(str);
		});*/
		staticFileLocation("/com/theiostream/orcamento/static");

		get("/", (request, response) -> {
			URL str = App.class.getResource("orgao-tree.html");
			return readFile(str);
		});
		get("/orgaos.json", (request, response) -> {
			if(true){
				String ret = "{\"name\": \"flare\", \"children\": [";
				
				ResIterator orgaos = db.getAllOrgaos();
				while (orgaos.hasNext()) {
					Resource orgao = orgaos.nextResource();
					Statement stmt = orgao.getProperty(ResourceFactory.createProperty(RDF2("label")));
					
					String name = stmt.getString();
					HashMap<String, Double> value = db.valueForDespesas(db.getDespesasForResource(orgao));
					
					String codigo = db.getCodigoForResource(orgao);

					ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] },");
				}
				
				ret = ret.substring(0, ret.length()-1);
				return ret.concat("]}");
			}

			else if(false) {
				URL str = App.class.getResource("orgaos.json");
				return readFile(str);
			}

			return null;
		});

		get("/r/:type/:org", (request, response) -> {
			URL str = App.class.getResource("Resource.html");
			URL js = App.class.getResource(request.params(":type") + ".js");
			
			if (js == null) return "Not Implemented. Check back later.";

			return readFile(str) + "\n<script>" + readFile(js) + "</script>";
		});
		get("/r/:type/:org/i", (request, response) -> {
			response.type("application/json");

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

			HashMap<String, Double> values = db.valueForDespesas(db.getDespesasForResource(res));
			
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
		get("/r/:type/:org/:par", (request, response) -> {
			response.type("application/json");

			String ret = "{\"name\": \"flare\", \"children\": [";
			
			String type = request.params(":type");
			String rtype = request.params(":par");
			Resource orgao = db.getResourceForCodigo(request.params(":org"), type);

			boolean sub = rtype.equals("Subtitulo");
			String p = request.queryParams("p");
			
			Iterator<OResource> functions = db.getOResourcesForResource(rtype, orgao);
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
				
				HashMap<String, Double> value;
				value = db.valueForDespesas(function.getDespesas());
				
				String codigo = db.getCodigoForResource(function.getResource());

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] },");
			}
			
			ret = ret.substring(0, ret.length()-1);
			return ret.concat("]}");			
		});
		
		get("/i/:p/:a/:s", (request, response) -> {
			URL str = App.class.getResource("Resource.html");			
			URL js = App.class.getResource("Subtitle.js");

			return readFile(str) + "\n<script>" + readFile(js) + "</script>";
		});
		get("/i/:p/:a/:s/i", (request, response) -> {
			response.type("application/json");
			
			Resource programa = db.getResourceForCodigo(request.params(":p"), "UnidadeOrcamentaria");
			Resource action = db.getResourceForCodigo(request.params(":a"), "Acao");
			Resource res = db.getSubtitleWithProgramaAndAcao(programa, action, request.params(":s"));

			Statement stmt = res.getProperty(ResourceFactory.createProperty(RDF2("label")));
			String name = stmt.getString();
			String parent = action.getProperty(ResourceFactory.createProperty(RDF2("label"))).getString();
			String pname = programa.getProperty(ResourceFactory.createProperty(RDF2("label"))).getString();

			HashMap<String, Double> values = db.valueForDespesas(db.getDespesasForResource(res));
			return "{ \"name\": \"" + parent + "\", \"parent\": \"" + name + "\", \"programa\": { \"name\": \"" + pname + "\" }, \"values\": { \"Valor LOA\": " + values.get("DotacaoInicial") + ", \"Valor Pago\": " + values.get("Pago") + "}}";

		});
		get("/i/:p/:a/:s/d", (request, response) -> {
			response.type("application/json");

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

		get("/l/:s", (request, response) -> {
			return null;
		});

		get("/c/:x/:y", (request, response) -> {
			return null;
		});
	}
}
