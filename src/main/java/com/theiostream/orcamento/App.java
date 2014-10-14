/* Orçamento Displayer
 * (c) 2014 Daniel Ferreira
 */

package com.theiostream.orcamento;

import static spark.Spark.*;
import com.theiostream.orcamento.Database;

import java.util.ArrayList;
import com.hp.hpl.jena.rdf.model.*;
import static com.theiostream.orcamento.OrcamentoUtils.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.StringBuilder;
import java.net.URL;

public class App  {
	public static void main(String[] args) {
		get("/", (request, response) -> {
			String ret;
			
			URL str = App.class.getResource("orgao-tree.html");
			
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
				System.out.println(exception);
				ret = "EXCEPTION.";
			}

			return ret;
		});
		
		get("/orgaos.json", (request, response) -> {
			String ret = "{\"name\": \"flare\", \"children\": [";
			Database db = new Database("");
			
			System.out.println("hi!");
			ResIterator orgaos = db.getAllOrgaos();
			while (orgaos.hasNext()) {
				Resource orgao = orgaos.nextResource();
				Statement stmt = orgao.getProperty(ResourceFactory.createProperty(RDF2("label")));
				
				String name = stmt.getString();
				double value = db.valueForDespesas(db.getDespesasForOrgao(orgao));

				ret = ret.concat("{ \"name\": \"" + name + "\", \"children\": [{ \"name\": \"" + name + "\", \"size\":" + value + "}] }");
				if (orgaos.hasNext()) ret += ",";
			}

			return ret.concat("]}");
		});
	}
}
