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

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.StringBuilder;
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

					ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] }");
					if (orgaos.hasNext()) ret += ",";
				}

				return ret.concat("]}");
			}

			else if(false) {
				URL str = App.class.getResource("orgaos.json");
				return readFile(str);
			}

			return null;
		});

		get("/r/:type/:org", (request, response) -> {
			URL str = App.class.getResource(request.params(":type") + ".html");
			return readFile(str);
		});
		get("/r/:type/:org/i", (request, respones) -> {
			Resource res = db.getResourceForCodigo(request.params(":org"), request.params(":type"));
			Statement stmt = res.getProperty(ResourceFactory.createProperty(RDF2("label")));
			return stmt.getString();
		});
		/*get("/r/:org/d", (request, response) -> {
			String ret = "{\"name\": \"flare\", \"children\": [";

			Resource res = db.getResourceForCodigo(request.params(":org"));
			ResIterator despesas = db.getDespesasForResource(res);
			
			while (despesas.hasNext()) {
				Resource despesa = despesas.nextResource();

				Resource action = db.getPropertyForDespesa(despesa, "Acao");
				Statement stmt = action.getProperty(ResourceFactory.createProperty(RDF2("label")));
				String name = stmt.getString();
				
				String codigo = db.getCodigoForResource(action);

				double dotInicial = db.getValorPropertyForDespesa(despesa, "DotacaoInicial");
				double pago = db.getValorPropertyForDespesa(despesa, "Pago");

				//ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + dotInicial + ", \"real\":" + pago + ", \"cod\": \"" + codigo + "\"" + "}] }");
				ret = ret.concat("{ \"name\": \"" + name + "\", \"size\":" + dotInicial + ", \"real\":" + pago + ", \"cod\": \"" + codigo + "\"" + " }");
				if (despesas.hasNext()) ret = ret.concat(",");
			}

			return ret.concat("]}");
		});*/		
		get("/r/:type/:org/:par", (request, response) -> {
			String ret = "{\"name\": \"flare\", \"children\": [";
			
			String type = request.params(":type");
			Resource orgao = db.getResourceForCodigo(request.params(":org"), type);
			
			Iterator<OResource> functions = db.getOResourcesForResource(request.params(":par"), orgao);
			while (functions.hasNext()) {
				OResource function = functions.next();
				Statement stmt = function.getResource().getProperty(ResourceFactory.createProperty(RDF2("label")));
					
				String name = stmt.getString();
				
				HashMap<String, Double> value;
				if (type.equals("Acao")) {
					double dotInicial = db.getValorPropertyForDespesa(function.getResource(), "DotacaoInicial");
					double pago = db.getValorPropertyForDespesa(function.getResource(), "Pago");

					value = new HashMap();
					value.put("DotacaoInicial", dotInicial);
					value.put("Pago", pago);

				}
				else value = db.valueForDespesas(function.getDespesas());
				
				String codigo = db.getCodigoForResource(function.getResource());

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] }");
				if (functions.hasNext()) ret = ret.concat(",");
			}

			return ret.concat("]}");			
		});

		get("/c/:x/:y", (request, response) -> {
			return null;
		});
	}
}
