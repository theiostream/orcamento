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
		Database db = new Database("2001");

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
					HashMap<String, Double> value = db.valueForDespesas(db.getDespesasForOrgao(orgao));
					
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

		get("/o/:org", (request, response) -> {
			/*Resource orgao = db.getOrgaoForCodigo(request.params(":org"));
			Statement stmt = orgao.getProperty(ResourceFactory.createProperty(RDF2("label")));
			return "Name is " + stmt.getString();*/

			URL str = App.class.getResource("unidade-tree.html");
			return readFile(str);
		});
		get("/o/:org/uo.json", (request, response) -> {
			String ret = "{\"name\": \"flare\", \"children\": [";
			Resource orgao = db.getOrgaoForCodigo(request.params(":org"));

			ResIterator unidades = db.getUnidadesForOrgao(orgao);
			while (unidades.hasNext()) {
				Resource unidade = unidades.nextResource();
				Statement stmt = unidade.getProperty(ResourceFactory.createProperty(RDF2("label")));
					
				String name = stmt.getString();
				HashMap<String, Double> value = db.valueForDespesas(db.getDespesasForUnidade(unidade));
				
				String codigo = db.getCodigoForResource(unidade);

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] }");
				if (unidades.hasNext()) ret += ",";
			}

			return ret.concat("]}");
		});
		get("/o/:org/fn.json", (request, response) -> {
			String ret = "{\"name\": \"flare\", \"children\": [";
			Resource orgao = db.getOrgaoForCodigo(request.params(":org"));
			
			Iterator<OResource> functions = db.getResourcesForOrgao("Funcao", orgao);
			while (functions.hasNext()) {
				OResource function = functions.next();
				Statement stmt = function.getResource().getProperty(ResourceFactory.createProperty(RDF2("label")));
					
				String name = stmt.getString();
				HashMap<String, Double> value = db.valueForDespesas(function.getDespesas());
				
				String codigo = db.getCodigoForResource(function.getResource());

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] }");
				if (functions.hasNext()) ret += ",";
			}

			return ret.concat("]}");
		});
		get("/o/:org/pr.json", (request, response) -> {
			String ret = "{\"name\": \"flare\", \"children\": [";
			Resource orgao = db.getOrgaoForCodigo(request.params(":org"));
			
			Iterator<OResource> functions = db.getResourcesForOrgao("Programa", orgao);
			while (functions.hasNext()) {
				OResource function = functions.next();
				Statement stmt = function.getResource().getProperty(ResourceFactory.createProperty(RDF2("label")));
					
				String name = stmt.getString();
				HashMap<String, Double> value = db.valueForDespesas(function.getDespesas());
				
				String codigo = db.getCodigoForResource(function.getResource());

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value.get("DotacaoInicial") + ", \"real\":" + value.get("Pago") + ", \"cod\": \"" + codigo + "\"" + "}] }");
				if (functions.hasNext()) ret += ",";
			}

			return ret.concat("]}");
		});
	}
}
