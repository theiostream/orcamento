// g.js
// d3.js <-> Orçamento Data bridge
// (c) 2014 Daniel Ferreira

// MUST IMPORT D3.JS BEFORE IMPORTING THIS
// I DISLIKE JAVASCRIPT
// NO I AM NOT PUTTING JQUERY HERE

// TODO: Maybe integrate createGraph()/createItemTable() functions into their reload() counterparts with some d3.js element managing

var ipca = "dez/14"

var programa;

/* Utilities {{{ */

function getURLParameter(name) {
  return /*decodeURIComponent(*/(new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search)||[,""])[1].replace(/\+/g, '%20')/*)*/||null
}
function documentURL() {
	return location.protocol + '//' + location.host + location.pathname;
}

/* Crazy Color Shit {{{ */
function d3_rgbNumber(value) {
  return new d3.rgb(value >> 16, value >> 8 & 0xff, value & 0xff);
}
function d3_rgbString(value) {
  return d3_rgbNumber(value) + "";
}
//var ti_category = [ 0xE1986B, 0x956547, 0xA64508, 0xE15700, 0x62422E, 0xE1E000, 0x959400, 0xA6A647, 0xE1E1A0, 0x626100, 0x1DE109, 0x139506, 0x50A647, 0xAFE1AA, 0x0D6204, 0x09B4E1, 0x067795, 0x4191A6, 0xAAD6E1, 0x044E62, 0xE1414E, 0x952B33, 0xA65158, 0xE19A9F, 0x8B2830, 0x0AE1A8, 0x07956F, 0x37A689, 0xB3E1D5, 0x068B67 ].map(d3_rgbString);
//var ti_category = [ 0xE1986B, 0x956547, 0xA64508, 0xE15700, 0x62422E, 0xE1E000, 0x959400, 0xA6A647, 0xE1E1A0, 0x626100, 0x1DE109, 0x139506, 0x50A647, 0xAFE1AA, 0x0D6204, 0x09B4E1, 0x067795, 0x4191A6, 0xAAD6E1, 0x044E62, 0xE1414E, 0x952B33, 0xA65158, 0xE19A9F, 0x8B2830, 0x0AE1A8, 0x07956F, 0x37A689, 0xB3E1D5, 0x068B67, 0x005BE1, 0x003C95, 0x4F72A6, 0x7DA5E1, 0x00388B, 0xE18600, 0x955900, 0xA68F6B, 0xE1992F, 0x8B5300, 0x5F0066, 0xD600E5, 0xE060EA, 0x633D66, 0xCD00DC ].map(d3_rgbString);
var ti_category = //[ 0xE1986B, 0x956547, 0xA64508, 0xE15700, 0x62422E, 0xE1E000, 0x959400, 0xA6A647, 0xE1E1A0, 0x626100, 0x1DE109, 0x139506, 0x50A647, 0xAFE1AA, 0x0D6204, 0x09B4E1, 0x067795, 0x4191A6, 0xAAD6E1, 0x044E62, 0xE1414E, 0x952B33, 0xA65158, 0xE19A9F, 0x8B2830, 0x0AE1A8, 0x07956F, 0x37A689, 0xB3E1D5, 0x068B67, 0x005BE1, 0x003C95, 0x4F72A6, 0x7DA5E1, 0x00388B, 0xE18600, 0x955900, 0xA68F6B, 0xE1992F, 0x8B5300, 0x5F0066, 0xD600E5, 0xE060EA, 0x633D66, 0xCD00DC, 0x0B660F, 0x18E522, 0x22EA2B, 0x5A665B, 0x17DC20 ].map(d3_rgbString);
	[ 0xE1986B, 0x62422E, 0x00388B, 0x17DC20, 0x956547, 0xA64508, 0xE15700, 0xE1E000, 0x959400, 0xA6A647, 0xE1E1A0, 0x626100, 0x1DE109, 0x139506, 0x50A647, 0xAFE1AA, 0x0D6204, 0x09B4E1, 0x067795, 0x4191A6, 0xAAD6E1, 0x044E62, 0xE1414E, 0x952B33, 0xA65158, 0xE19A9F, 0x8B2830, 0x0AE1A8, 0x07956F, 0x37A689, 0xB3E1D5, 0x068B67, 0x005BE1, 0x003C95, 0x4F72A6, 0x7DA5E1, 0xE18600, 0x955900, 0xA68F6B, 0xE1992F, 0x8B5300, 0x5F0066, 0xD600E5, 0xE060EA, 0x633D66, 0xCD00DC, 0x0B660F, 0x18E522, 0x22EA2B, 0x5A665B ].map(d3_rgbString);
function hexToRgb(hex) {
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
}
/* }}} */

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

/* }}} */

function fillInfo() {
	var header = document.getElementById("header");

	getJSON(documentURL() + "/i", function(info){
		var tp = document.createElement("h4");
		tp.setAttribute('style', 'color: #837E7C;');
		tp.innerHTML = documentURL().split('/').slice(-2, -1)[0];
		header.appendChild(tp);

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

function addHeader(tit) {
	var row = document.createElement("div");
	row.setAttribute("class", "row");
	row.innerHTML = '<div class="cell"><h3>' + tit + '</h3></div>';

	var container = document.getElementsByClassName("maintable")[0];
	container.appendChild(row);
}

/* Table & Treemap (Resource) {{{ */

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
		+ '			<div style="position: relative; height: calc(100% - 38px);"><div style="overflow-y:scroll; position:absolute; top:0;right:0;left:0;bottom:0;">'
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
	d3.json(documentURL() + "/d", function(data) {
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
	//var color = d3.scale.category20();
	var color;
	if (type.lastIndexOf("a", 0)===0) color = d3.scale.ordinal().range(ti_category);
	else color = d3.scale.category20();

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
	if (type.lastIndexOf("a", 0)===0) {
		type = type.substring(1);
		url = documentURL() + "/r";
	}
	else {
		url = documentURL() + "/" + type;
		console.log("pre url is " + url);
		if (type == "Subtitulo") url += "?p=" + programa;
		if (getURLParameter("f")) url += (url.indexOf("?")>-1 ? "&" : "?") + "f=" + getURLParameter("f");
	}
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
				var c = hexToRgb(color(d.name));
				return d.children ? "rgba(" + [c.r, c.g, c.b, 1.0].join() + ")" : null; 
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
				this.parentNode.style.background = "rgba(0, 0, 0, 0.8)";
			})
			.on('mouseout', function(d) {
				this.parentNode.style.background = "rgba(0, 0, 0, 1)";
			})
			.on('click', function(d) {
				if (type == "Subtitulo") window.location = "/i/" + programa + "/" + documentURL().substr(documentURL().lastIndexOf('/') + 1) + "/" + d.cod;
				else {
					var year = documentURL().split('/')[4];
					var wl = "/r/" + year + "/" + type + "/" + d.cod;

					if (document.getElementById("hierarchy").checked) {
						/* this is horrible i know i just want this to work this whole thing needs a cleanup in fact */
						var obj = {};
						obj[documentURL().split('/')[4]] = documentURL().split('/')[5];
						if (getURLParameter("f")) {
							var j = JSON.parse(decodeURIComponent(getURLParameter("f")));
							for (var a in j) { obj[a] = j[a]; }
						}
						console.log(obj);

						wl += "?f=" + encodeURIComponent(JSON.stringify(obj));
					}
					
					console.log("wl = " + wl);
					window.location = wl;
				}
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

/* }}} */

/* Action (Programa selector) {{{ */

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
		
		getJSON(documentURL() + "/UnidadeOrcamentaria", function(info){
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

/* }}} */

/* History {{{ */

var translation = {
	"loa": "LOA",
	"pago": "Pago",
	"aumloa": "Aumento (LOA)",
	"aumpago": "Aumento (Pago)",
	"inf": "Inflação (IPCA)"
};

function createGraphHistory(id, tit) {
	var row = document.createElement("div");
	row.setAttribute("class", "row");
	
	var g = document.createElement("div");
	g.setAttribute("id", id + "-history");
	//g.setAttribute("class", "table table-hover table-condensed legenda");
	g.setAttribute("style", "height: 500px;");
	
	row.innerHTML =
		  '<div class="cell">'
		+ '	<div class="btn-group pull-right prfix" id="' + id + '-group">'
		+ '		<button type="button" class="btn btn-default btn-sm active" data-key="noinf">Valores Nominais</button>'
		+ '		<button type="button" class="btn btn-default btn-sm" data-key="inf">Valores Corrigidos (IPCA)</button>'
		+ '	</div>'
		+ g.outerHTML
		+ '</div>';
	
	var container = document.getElementsByClassName("maintable")[0];
	container.appendChild(row);
}

function reloadDataHistory(id, rinfo) {
	var el = document.getElementById(id + "-history");
	
	d3.json(documentURL() + "/d?rinfo=" + rinfo, function(error, data) {
		data.sort(function(a,b){ return parseInt(a.letter)>parseInt(b.letter)?1:parseInt(a.letter)<parseInt(b.letter)?-1:0; });
	
		var ageNames = d3.keys(data[0]).filter(function(key) { return key=="loa" || key=="pago"; });
		data.forEach(function(d) {
			d.ages = ageNames.map(function(name) { return {name: name, value: +d[name]}; });
		});
		reloadHistory_(id, el, data, ageNames);
		
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
				
				var btnkey = this.getAttribute("data-key");
				var ageNames = d3.keys(data[0]).filter(function(key) {
					if (btnkey == "noinf"){console.log("WOOT"); return key=="loa" || key=="pago";}
					return key=="infloa" || key=="infpago";
				});
				console.log(ageNames);
				data.forEach(function(d) {
					d.ages = ageNames.map(function(name) { return {name: name, value: +d[name]}; });
				});

				el.innerHTML = "";
				reloadHistory_(id, el, data, ageNames);
			}
		});
	});
}

function reloadHistory_(id, el, data, ageNames) {
	var margin = {top: 20, right: 20, bottom: 50, left: 40};
	var width = el.offsetWidth - margin.left - margin.right;
	var height = el.offsetHeight - margin.top - margin.bottom;

	var x0 = d3.scale.ordinal().rangeRoundBands([0, width-60], .1);
	var x1 = d3.scale.ordinal();
	var y = d3.scale.linear().range([height, 0]);

	var color = d3.scale.ordinal().range(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

	var xAxis = d3.svg.axis()
		.scale(x0)
		.orient("bottom");

	var yAxis = d3.svg.axis()
		.scale(y)
		.orient("left")
		.tickFormat(d3.format(".2s"));

	var svg = d3.select("#" + id + "-history").append("svg")
		.attr("width", width + margin.left + margin.right)
		.attr("height", height + margin.top + margin.bottom)
		.append("g")
		.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

	x0.domain(data.map(function(d) { return d.letter; }));
	x1.domain(ageNames).rangeRoundBands([0, x0.rangeBand()]);
	y.domain([0, d3.max(data, function(d) { return d3.max(d.ages, function(d) { return d.value; }); })]);

	svg.append("g")
		.attr("class", "x axis")
		.attr("transform", "translate(0," + height + ")")
		.call(xAxis);

	svg.append("g")
		.attr("class", "y axis")
		.call(yAxis)
		.append("text")
		.attr("transform", "rotate(-90)")
		.attr("y", 6)
		.attr("dy", ".71em")
		.style("text-anchor", "end")
		.text("Valor");

	var state = svg.selectAll(".state")
		.data(data)
		.enter().append("g")
		.attr("class", "g")
		.attr("transform", function(d) { return "translate(" + x0(d.letter) + ",0)"; });

	state.selectAll("rect")
		.data(function(d) { return d.ages; })
		.enter().append("rect")
		.attr("width", x1.rangeBand())
		.attr("x", function(d) { return x1(d.name); })
		.attr("y", function(d) { return y(d.value); })
		.attr("height", function(d) { return height - y(d.value); })
		.style("fill", function(d) { return color(d.name); });

	var legend = svg.selectAll(".legend")
		.data(ageNames.slice().reverse())
		.enter().append("g")
		.attr("class", "legend")
		.attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

	legend.append("rect")
		.attr("x", width - 18)
		.attr("width", 18)
		.attr("height", 18)
		.style("fill", color);

	legend.append("text")
		.attr("x", width - 24)
		.attr("y", 9)
		.attr("dy", ".35em")
		.style("text-anchor", "end")
		.style("font-size", "10px")
		.text(function(d) { return translation[d]; });
	
	svg.append("text")
		.style("font-size", "10px")
		.style("text-align", "center")
		.attr("width", width)
		.attr("y", height + 30)
		.text("Nota: Correções inflacionárias (IPCA-E) de valores da são realizadas entre janeiro do ano referido e " + ipca + " (mais recente divulgação do índice).");
}

/* }}} */
