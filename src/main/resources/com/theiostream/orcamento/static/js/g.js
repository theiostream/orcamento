// g.js
// d3.js <-> Orçamento Data bridge
// (c) 2014 Daniel Ferreira

// MUST IMPORT D3.JS BEFORE IMPORTING THIS
// I DISLIKE JAVASCRIPT
// NO I AM NOT PUTTING JQUERY HERE

// TODO: Maybe integrate createGraph()/createItemTable() functions into their reload() counterparts with some d3.js element managing

var programa;

function getJSON(url, f) {
	var xhr = new XMLHttpRequest();
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status == 200) {
			var info = JSON.parse(xhr.responseText);
			f(info);
		}
	};

	xhr.open("GET", url, true);
	xhr.send();
}

function dots(v) {
	return Math.round(v).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

function fillInfo() {
	var header = document.getElementById("header");

	getJSON(document.URL + "/i", function(info){
		var title = document.createElement("h1");
		title.innerHTML = info.name + " <small>" + info.parent + "</small>";
		header.appendChild(title);
		
		if (info.programa) {
			programa = info.programa.cod;
			info.values["Programa"] = info.programa.name;
		}

		for (var key in info.values) {
			var p = document.createElement("p");
			p.setAttribute("class", "lead");
			p.innerHTML = "<b>" + key + "</b>&nbsp;&nbsp;";
			
			if (isNaN(info.values[key])) p.innerHTML += info.values[key];
			else p.innerHTML += "R$" + Math.round(info.values[key]).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".") + ",00";

			header.appendChild(p);
		}

	});
}

function makePrograma(a, cod) {
	var siblings = a.parentNode.parentNode.children;
	for (var i=0; i<siblings.length; i++) {
		if (siblings[i].classList.contains("active")) {
			siblings[i].classList.remove("active");
			break;
		}
	}

	a.parentNode.classList.add("active");
	
	programa = cod;
	reloadData("st", "Subtitulo");
}

function addProgramaSelector() {
	var warning = "Mais de uma unidade orçamentária realiza esta ação. Selecione uma delas para visualizar mais detalhes."

	if (!programa) {
		var row = document.createElement("div");
		row.setAttribute("class", "row");

		var pselector = document.createElement("ul");
		pselector.setAttribute("id", "pselector");
		pselector.setAttribute("class", "nav navbar-nav");
		
		getJSON(document.URL + "/UnidadeOrcamentaria", function(info){
			var p = info.children;
			for (var i=0; i<p.length; i++) {
				var n = p[i].name;
				var c = p[i].children[0].cod;
				var v = p[i].children[0].size;

				var li = document.createElement("li");
				li.innerHTML = '<a onclick="makePrograma(this, \'' + c + '\');">' + n + '<br><small>R$' + dots(v) + '</small></a>';

				pselector.appendChild(li);

				row.innerHTML =
					  '<div class="cell"><div class="panel panel-warning">'
					+ '	<div class="panel-heading"><h3 class="panel-title">Unidade Orçamentária <small>' + warning + '</small></h3></div>'
					+ '	<div class="panel-body"><nav class="bs-docs-sidebar">'
					+		pselector.outerHTML
					+ '	</ul></div>'
					+ '</div></div>';

			}
		});
				
		var container = document.getElementsByClassName("maintable")[0];
		container.appendChild(row);
	}
}

function createGraph_(id, tit, sz) {
	var row = document.createElement("div");
	row.setAttribute("class", "row");
	
	var table = document.createElement("table");
	table.setAttribute("id", id + "-legenda");
	table.setAttribute("class", "table table-hover table-condensed legenda");

	var treemap = document.createElement("div");
	treemap.setAttribute("class", "treemap");
	treemap.setAttribute("id", id + "-treemap");
	
	row.innerHTML =
		  '<div class="cell">'
		+ '	<div class="graphcontent ' + sz + '">'
		+ '		<div class="panel panel-default legendac">'
		+ '			<div class="panel-heading">'
		+ '				<div class="btn-group pull-right prfix" id="' + id + '-group">'
		+ '					<button type="button" class="btn btn-default btn-sm active" data-key="size">LOA</button>'
		+ '					<button type="button" class="btn btn-default btn-sm" data-key="real">Pago</button>'
		+ '				</div>'
		+ '				<h3 class="panel-title">'
		+					tit
		+ '				</h3>'
		+ '			</div>'
		+ '			<div style="position: relative; height: 88%;"><div style="overflow-y:scroll; position:absolute; top:0;right:0;left:0;bottom:0;">'
		+ 				table.outerHTML
		+ '			</div></div>'
		+ '		</div>'
		+ 		treemap.outerHTML
		+ '	</div>'
		+ '</div>';
	
	var container = document.getElementsByClassName("maintable")[0];
	container.appendChild(row);
}

function createGraph(id, tit) { createGraph_(id, tit, "smallgraph"); }
function createBigGraph(id, tit) { createGraph_(id, tit, "biggraph"); }

// this is very very ugly help help FIXME
function createItemTable(id) {
	var row = document.createElement("div");
	row.setAttribute("class", "row");
	
	var label = document.createElement("h3");
	label.setAttribute("id", id + "-itemslabel");
	row.appendChild(label);

	var table = document.createElement("table");
	table.setAttribute("id", id + "-items");
	table.setAttribute("class", "table table-hover table-bordered items sortable");
	row.appendChild(table);

	var container = document.getElementsByClassName("maintable")[0];
	container.appendChild(row);
}

function populateItemTables(ids) {
	d3.json(document.URL + "/d", function(data) {
		var columns = ["Plano Orçamentário", "Modalidade de Aplicação", "Valor"];
		
		for (var i = 0; i<ids.length; i++) {
			document.getElementById(ids[i] + "-itemslabel").innerHTML = Object.keys(data)[i];

			var table = d3.select("#" + ids[i] + "-items");
			var thead = table.append("thead");
			var tbody = table.append("tbody");

			thead.append("tr")
				.selectAll("th")
				.data(columns)
				.enter().append("th")
				.text(function(d) { return d; });

			var rows = tbody.selectAll("tr")
				.data(data[Object.keys(data)[i]])
				.enter().append('tr');

			var cells = rows.selectAll('td')
				.data(function(row) {
					return columns.map(function(column) {
						return { column: column, value: row[column] };
					});
				})
				.enter().append('td')
				.text(function(d) {
					if (isNaN(d.value)) return d.value;
					return Math.round(d.value).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
				});
			
			new Tablesort(document.getElementById(ids[i] + "-items"));
		}
	});
}

var nodes = [];
function reloadData(id, type) {
	var color = d3.scale.category20();

	var uoDiv_ = document.getElementById(id + "-treemap");
	while (uoDiv_.hasChildNodes()) uoDiv_.removeChild(uoDiv_.lastChild);

	var uoDiv = d3.select("#" + id + "-treemap");

	var uoTreemap = d3.layout.treemap()
		.size([uoDiv_.offsetWidth, uoDiv_.offsetHeight])
		.sticky(true)
		.value(function(d) { return d.size; });
	
	var uoTable_ = document.getElementById(id + "-legenda");
	while (uoTable_.hasChildNodes()) uoTable_.removeChild(uoTable_.lastChild);

	var uoTable = d3.select("#" + id + "-legenda");
	var uoThead = uoTable.append('thead');
	var uoTbody = uoTable.append('tbody');
	
	var url;
	if (type.lastIndexOf("a", 0)===0) url = document.URL + "/r";
	else url = type=="Subtitulo" ? document.URL + "/" + type + "?p=" + programa : document.URL + "/" + type;
	console.log(url);

	d3.json(url, function(error, root) {
		var nodz = {};
		
		var uoNodes = uoDiv.datum(root).selectAll(".node")
			.data(uoTreemap.nodes)
			.enter().append("div")
			.attr("class", "node")
			.call(position)
			.style("background", function(d) {
				if(d.name=="flare") return "#ffffff";
				return d.children ? color(d.name) : null; 
			})
			.text(function(d) {
				if (d.name == "flare") return "";
				if (!d.children) return null;
				
				nodz[d.name] = this;
				if (this.offsetWidth*this.offsetHeight < getTextWidth(d.name)*10) return "";
				return d.name;
			})
			.on('mouseover', function(d) {
				//this.parentNode.appendChild(this);
				// highlight
			})
			.on('mouseout', function(d) {
				// unhighlight
			})
			.on('click', function(d) {
				if (type == "Subtitulo") window.location = "/i/" + programa + "/" + document.URL.substr(document.URL.lastIndexOf('/') + 1) + "/" + d.cod;
				else window.location = "/r/" + type + "/" + d.cod;
			});
		nodes.push(uoNodes);
		
		var columns = ["name", "value"];
		uoThead.append('tr')
			.selectAll('th')
			.data(columns)
			.enter().append('th')
			.text(function(column) { return column.localeCompare("name")==0 ? "Nome" : "Valor (R$)"; });

		var rows = uoTbody.selectAll('tr')
			.data(root.children)
			.enter().append('tr');
		var cells = rows.selectAll('td')
			.data(function(row) {
				return columns.map(function(column) {
					return {column: column, value: row[column]};
				});
			})
			.enter().append('td')
			.text(function(d) {
				if (isNaN(d.value)) return d.value;
				return d.value.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
			})
			/*.on('mouseover', function(d) {
				uoNodes.style('opacity', .1);
				d3.select(nodz[d.value]).style('opacity', 1);
			})*/;

		d3.selectAll(document.getElementById(id + "-group").children).on('click', function() {
			if (!this.classList.contains("active")) {
				var siblings = this.parentNode.children;
				for (var i=0; i<siblings.length; i++) {
					if (siblings[i].classList.contains("active")) {
						siblings[i].classList.remove("active");
						break;
					}
				}
				
				this.classList.add("active");
				
				var key = this.getAttribute("data-key");
				var f = function(d) { return d[key]; };
				uoNodes
					.data(uoTreemap.value(f).nodes)
					.transition()
					.duration(1000)
					.call(position);
				
				cells
					.data(function(row) {
						return columns.map(function(column) {
							return {column: column, value: row[column]};
						});
					})
					.text(function(d) {
						if (isNaN(d.value)) return d.value;
						return Math.round(d.value).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
					});
			}
		});
	});
}

function position() {
  this.style("left", function(d) { return d.x + "px"; })
      .style("top", function(d) { return d.y + "px"; })
      .style("width", function(d) { return Math.max(0, d.dx - 1) + "px"; })
      .style("height", function(d) { return Math.max(0, d.dy - 1) + "px"; });
}

function getTextWidth(text) {
	ctx = document.createElement('canvas').getContext('2d');
	ctx.font = "10px sans-serif";
	return ctx.measureText(text).width;
}

