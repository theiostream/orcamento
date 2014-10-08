/* Orçamento Displayer
 * (c) 2014 Daniel Ferreira
 */

package com.theiostream.orcamento;

import static spark.Spark.*;
import com.theiostream.orcamento.Database;

public class App  {
	public static void main(String[] args) {
		get("/", (request, response) -> {
			Database x = new Database("");
			x.getOrgans();

			return "lol";
		});
	}
}
