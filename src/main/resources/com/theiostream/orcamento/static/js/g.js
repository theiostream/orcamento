// g.js
// d3.js <-> Orçamento Data bridge
// (c) 2014 Daniel Ferreira

// MUST IMPORT D3.JS BEFORE IMPORTING THIS
// I DISLIKE JAVASCRIPT
// NO I AM NOT PUTTING JQUERY HERE
// NEVERMIND FUCK YOU TWITTER TYPEAHEAD

// TODO: Maybe integrate createGraph()/createItemTable() functions into their reload() counterparts with some d3.js element managing

var ipca = "dez/14"
var translatetype = {
	"Orgao": "Órgão",
	"UnidadeOrcamentaria": "Unidade Orçamentária",
	"Funcao": "Função",
	"Subfuncao": "Subfunção",
	"Programa": "Programa",
	"Acao": "Ação"
};

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

var ti_category = [ 0xc05746, 0x86a076, 0xa1ce5e, 0x445235, 0x272048,
		    0x40f99b, 0x39304a, 0x635c51, 0x7d7461, 0xb0a990,
		    0x870058, 0xa4303f, 0xf2d0a4, 0xffeccc, 0xc86daf]
.map(d3_rgbString);

var color = d3.scale.ordinal().range(ti_category);

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

function urlinfo() {
	var s = location.pathname.split('/');
	return {
		req: s[1],
		year: s[2],
		type: s[3],
		cod: s[4]
	};
}

/* }}} */

/* Info / Header {{{ */

function fillInfo() {
	var i = urlinfo();
	var header = document.getElementById("header");

	getJSON(documentURL() + "/i", function(info){
		var tp = document.createElement("p");
		tp.setAttribute('style', 'color: #757575; font-size: 14pt;');
		tp.innerHTML = translatetype[documentURL().split('/').slice(-2, -1)[0]];
		header.appendChild(tp);

		var title = document.createElement("p");
		title.className = 'title';
		title.innerHTML = info.name// + " <small>" + info.parent + "</small>";
		header.appendChild(title);
		
		if (info.programa) {
			programa = info.programa.cod;
			info.values["Unidade Orçamentária"] = info.programa.name;
		}

		for (var key in info.values) {
			var p = document.createElement("p");
			p.setAttribute("class", "lead");
			p.innerHTML = '<span style="color: #757575;">' + key + "</span>&nbsp;&nbsp;";
			
			if (isNaN(info.values[key])) p.innerHTML += info.values[key];
			else p.innerHTML += 'R$' + Math.round(info.values[key]).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".") + ",00";

			header.appendChild(p);
		}
	});

	setSearchyear(i.year);
	
	if (i.req == "r") {
		var hurl = "/h/" + i.type + "/" + i.cod;
		var gurl = "/g/" + i.year + "/" + i.type + "/" + i.cod;

		var hb = document.getElementById("headerbtn");
		hb.innerHTML =
			  '<div class="btn-group-vertical">'
			+ '	<a class="btn btn-default" href="' + hurl + '" data-title="Despesas Históricas"><span class="glyphicon glyphicon-stats"></span></button>'
			+ '	<a class="btn btn-default" href="#" data-title="Portal da Transparência"><span class="glyphicon glyphicon-link"></span></button>'
			+ '</div>';
	}
	
	addTypeahead($("#search"), true);
}

function addHeader(tit) {
	var row = document.createElement("div");
	row.setAttribute("class", "row");
	row.innerHTML = '<div class="cell"><span class="subtitle">' + tit + '</span></div>';

	var container = document.getElementsByClassName("maintable")[0];
	container.appendChild(row);
}

/* }}} */

/* Table & Treemap (Resource) {{{ */

/* Treemap {{{ */
function preloadTreemap(uoDiv_) {
	return uoTreemap = d3.layout.treemap()
		.size([uoDiv_.offsetWidth, uoDiv_.offsetHeight])
		.sticky(true)
		.value(function(d) { return d.size; });
}
function reloadTreemap(id, color, uoDiv_, uoDiv, data, uoTreemap) {
	if (data == null) data = datacache[id];
	else datacache[id] = data;

	uoDiv_.innerHTML = "";

	var key;
	var s = document.getElementById(id + "-group").children;
	for (var i=0; i<s.length; i++) {
		if (s[i].classList.contains("active")) {
			key = s[i].getAttribute("data-key");
			break;
		}
	}

	return uoDiv.datum(data).selectAll(".node")
		.data(uoTreemap.value(function(d) { return d[key]; }).nodes)
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
			
			if (this.offsetWidth*this.offsetHeight < getTextWidth(d.name)*10) return "";
			return d.name;
		})
		.on('mouseover', function(d) {
		})
		.on('mouseout', function(d) {
		});
}
/* }}} */

/* Bubbles {{{ */

function preloadBubbles(div) {
	console.log("[pld] wid " + div.offsetWidth + " height is " + div.offsetHeight);

	return d3.layout.pack()
		.sort(null)
		.size([div.offsetWidth, div.offsetHeight])
		.padding(1.5);
}

function reloadBubbles(id, color, uoDiv_, uoDiv, data, bubble) {
	function redraw() {
		vis.attr("transform", "translate(" + d3.event.translate + ")" + " scale(" + d3.event.scale + ")");
	}

	if (data == null) data = datacache[id];
	else datacache[id] = data;

	uoDiv_.innerHTML = "";
	console.log("[ld] wid " + uoDiv_.offsetWidth + " height is " + uoDiv_.offsetHeight);

	var key;
	var s = document.getElementById(id + "-group").children;
	for (var i=0; i<s.length; i++) {
		if (s[i].classList.contains("active")) {
			key = s[i].getAttribute("data-key");
			break;
		}
	}
	console.log("key is " + key);
	
	var w = uoDiv_.offsetWidth;
	var h = uoDiv_.offsetHeight;
	console.log("Variables w="+w+"; h="+h);

	var vis = uoDiv.append("svg")
		.attr("width", w)
		.attr("height", h)
		.attr("pointer-events", "all")
		.style("height", h)
		.attr("class", "bubble")
		.call(d3.behavior.zoom()
			.xExtent([0, 1])
			.yExtent([0, 1])
			.scaleExtent([1, 10])
			.on("zoom", redraw))
		.append('g').attr('class', 'group2');
	
	var x = vis
		.selectAll(".nd")
		.data(bubble.value(function(d) { console.log(d); return d[key]; }).nodes(classes(data)).filter(function(d) { return !d.children; }))
		.enter().append("g")
		.attr("class", "nd")
		.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
	
	x.append("title").text(function(d) { return d.name; })
	x.append("circle").attr("r", function(d){return d.r;}).style("fill", function(d){return color(d.packageName);})
	x.append("text")
		.attr("text-anchor", "middle")
		.attr("style", function(d) {var szd = d.r/5;return "font-size:" + szd+"px";})
		.each(function(d, i) {
			var nm = d.name;
			var arr = nm.replace(/[\(\)\\/,-]/g, " ").replace(/\s+/g, " ").split(" "),arrlength = (arr.length > 7) ? 8 : arr.length;
			d3.select(this).attr('y',"-" + (arrlength/2) + "em");
			
			//if text is over 7 words then ellipse the 8th
			for(var n = 0; n < arrlength; n++) {
				var tsp = d3.select(this).append('tspan').attr("x", "0").attr("dy", "1em");
				if(n === 7) {
					tsp.text("...");
				}
				else {
					tsp.text(arr[n]);
				}
			}
		});
		
		//.text(function(d){return d.className.substring(0, d.r/3);});

	return x;
}

/* }}} */

var graphs = { "sq": [preloadTreemap, reloadTreemap], "bu": [preloadBubbles, reloadBubbles] };
var datacache = {};

/* Create Graph {{{ */

function createGraph_(id, tit, type, sz) {
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
		+ '				<span class="panel-title">'
		+					tit
		+ '				</span>'
		+ '			</div>'
		+ '			<div style="position: relative; height: calc(100% - 42px);"><div style="overflow-y:scroll; position:absolute; top:0;right:0;left:0;bottom:0;">'
		+ 				table.outerHTML
		+ '			</div></div>'
		+ '		</div>'
		+ 		treemap.outerHTML
		+ '		<div class="tmselection">'
		+ '			<div class="btn-group btn-group-xs btn-group-vertical" id="' + id + '-sel">'
		+ '				<button type="button" class="btn btn-default active" data-key="sq">&#9634;</button>'
		+ '				<button type="button" class="btn btn-default" data-key="bu">&#9711;</button>'
		+ '			</div>'
		+ '		</div>'
		+ '	</div>'
		+ '</div>';
	
	var container = document.getElementsByClassName("maintable")[0];
	container.appendChild(row);

	d3.selectAll(document.getElementById(id + "-sel").children).on('click', function(d) {
		if (!this.classList.contains("active")) {
			var siblings = this.parentNode.children;
			for (var i=0; i<siblings.length; i++) {
				if (siblings[i].classList.contains("active")) {
					siblings[i].classList.remove("active");
					break;
				}
			}
			this.classList.add("active");
			
			// TODO find a way to avoid getElementById()-ing every time
			var g = this.getAttribute("data-key");
			var uoDiv_ = document.getElementById(id + "-treemap");
			var uoDiv = d3.select(uoDiv_);
			
			// FIXME dont make copy
			var click = function(d) {
				var i = urlinfo();
				if (type == "Subtitulo") window.location = "/i/" + i.year + "/" + programa + "/" + i.cod + "/" + d.cod;
				else {
					var wl = "/r/" + i.year + "/" + type + "/" + d.cod;

					/*if (document.getElementById("hierarchy").checked) {
						//this is horrible i know i just want this to work this whole thing needs a cleanup in fact
						var obj = {};
						obj[documentURL().split('/')[4]] = documentURL().split('/')[5];
						if (getURLParameter("f")) {
							var j = JSON.parse(decodeURIComponent(getURLParameter("f")));
							for (var a in j) { obj[a] = j[a]; }
						}
						console.log(obj);

						wl += "?f=" + encodeURIComponent(JSON.stringify(obj));
					}*/
					
					window.location = wl;
				}
			}

			var preloadGraph = graphs[g][0];
			var reloadGraph = graphs[g][1];
			reloadGraph(id, color, uoDiv_, uoDiv, null, preloadGraph(uoDiv_)).on('click', click);
		}

	});
}
function createGraph(id, tit, type) { createGraph_(id, tit, type, "smallgraph"); }
function createBigGraph(id, tit, type) { createGraph_(id, tit, type, "biggraph"); }

// TODO make this prettier (?)
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

/* }}} */

/* Reload Data {{{ */

function reloadData(id, type) { reloadDataG(id, type, "sq"); }

var nodes = [];
function reloadDataG(id, type, g) {
	var uoDiv_ = document.getElementById(id + "-treemap");
	var uoDiv = d3.select("#" + id + "-treemap");
	
	var preloadGraph = graphs[g][0];
	var reloadGraph = graphs[g][1];

	var uoTreemap = preloadGraph(uoDiv_);
	
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
	
	var click = function(d) {
		var i = urlinfo();
		if (type == "Subtitulo") window.location = "/i/" + i.year + "/" + programa + "/" + i.cod + "/" + d.cod;
		else {
			var wl = "/r/" + i.year + "/" + type + "/" + d.cod;

			/*if (document.getElementById("hierarchy").checked) {
				//this is horrible i know i just want this to work this whole thing needs a cleanup in fact
				var obj = {};
				obj[documentURL().split('/')[4]] = documentURL().split('/')[5];
				if (getURLParameter("f")) {
					var j = JSON.parse(decodeURIComponent(getURLParameter("f")));
					for (var a in j) { obj[a] = j[a]; }
				}
				console.log(obj);

				wl += "?f=" + encodeURIComponent(JSON.stringify(obj));
			}*/
			
			window.location = wl;
		}
	}

	d3.json(url, function(error, root) {
		var uoNodes = reloadGraph(id, color, uoDiv_, uoDiv, root, uoTreemap).on('click', click);
		nodes.push(uoNodes);
		
		var columns = ["name", "size"];
		uoThead.append('tr')
			.selectAll('th')
			.data(columns)
			.enter().append('th')
			.text(function(column) { return column.localeCompare("name")==0 ? "Nome" : "Valor (R$)"; });

		var rows = uoTbody.selectAll('tr')
			.data(classes(root).children)
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
			.on('mouseover', function(d) {
				// implement
			});

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
				
				var g;
				var sel = document.getElementById(id + "-sel").children;
				for (var i=0; i<sel.length; i++) {
					if (sel[i].classList.contains("active")) {
						g = sel[i].getAttribute("data-key");
						break;
					}
				}
				
				var p = graphs[g][0];
				var r = graphs[g][1];
				r(id, color, uoDiv_, uoDiv, root, p(uoDiv_)).on('click', click);
				
				columns = ["name", this.getAttribute("data-key")];
				cells
					.data(function(row) {
						return columns.map(function(column) {
							console.log(row);
							return {column: column, value: row[column]};
						});
					})
					.text(function(d) {
						if (isNaN(d.value)) return d.value;
						return Math.round(d.value).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
					});
			}
		});
		console.log("[3] wid " + uoDiv_.offsetWidth + " height is " + uoDiv_.offsetHeight);
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

function classes(root) {
  var classes = [];

  function recurse(name, node) {
    if (node.children) node.children.forEach(function(child) { recurse(node.name, child); });
    else classes.push({packageName: name, name: node.name, size: node.size, real: node.real, cod: node.cod});
  }

  recurse(null, root);
  return {children: classes};
}

/* }}} */

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
