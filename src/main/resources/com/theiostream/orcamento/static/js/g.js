// g.js
// d3.js <-> Orçamento Data bridge
// (c) 2014 Daniel Ferreira

// MUST IMPORT D3.JS BEFORE IMPORTING THIS
// I DISLIKE JAVASCRIPT
// NO I AM NOT PUTTING JQUERY HERE
// NEVERMIND FUCK YOU TWITTER TYPEAHEAD

// TODO: Maybe integrate createGraph()/createItemTable() functions into their reload() counterparts with some d3.js element managing

var ipca = "mar/15"
var translatetype = {
	"Orgao": "Órgão",
	"UnidadeOrcamentaria": "Unidade Orçamentária",
	"Funcao": "Função",
	"Subfuncao": "Subfunção",
	"Programa": "Programa",
	"Acao": "Ação"
};
var translation = {
	"loa": "LOA",
	"pago": "Pago",
	"infloa": "LOA",
	"infpago": "Pago"
};

var programa;
var slideout;
var filter = {};
var hierarchy = false;
var info;

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

var ti_category = [ 0xc05746, 0x86a076, 0xa1ce5e, 0x445235, 0x89b6a5,
		    0x40f99b, 0xe6efe9, 0x2e79ba, 0xa1c1dd, 0xb0a990,
		    0x870058, 0xa4303f, 0xf2d0a4, 0xffeccc, 0xc86daf,
		    0xe4fde1, 0x8acb88, 0x648381, 0xffbf46, 0x3e8989 ]
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
	
	if (getURLParameter("h") == "1") {
		hierarchy = true;
	}

	getJSON(documentURL() + "/i" + (hierarchy ? "?f="+getURLParameter("f") : ""), function(info_){
		info = info_;

		if (i.req != 'a') {
			var tp = document.createElement("p");
			tp.setAttribute('style', 'color: #757575; font-size: 14pt;');
			tp.innerHTML = translatetype[i.req=="i" ? "Acao" : (i.req=="r" ? i.type : i.year)];
			header.appendChild(tp);
		}

		var title = document.createElement("p");
		title.className = 'title';
		title.innerHTML = info.name + (info.parent ? (" <small>" + info.parent + "</small>") : "");
		header.appendChild(title);
		
		if (info.programa) {
			programa = info.programa.cod;
			info.values["Unidade Orçamentária"] = info.programa.name;
		}

		for (var key in info.values) {
			var p = document.createElement("p");
			p.setAttribute("class", "lead");
			p.innerHTML = '<span style="color: #757575;">' + key + "</span>&nbsp;&nbsp;";
			
			if ((i.req != "r" && i.req != "a" && i.req != "i") || isNaN(info.values[key])) p.innerHTML += info.values[key];
			else p.innerHTML += 'R$' + Math.round(info.values[key]).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".") + ",00";

			header.appendChild(p);
		}
		
		if (i.req == "r") {
			if (hierarchy && getURLParameter("f")) {
				var bc = $("#bc");
				bc.css('display', 'table');
				bc.css('width', '100%');

				var h = JSON.parse(decodeURIComponent(getURLParameter("f")));

				for (var x=0; x<h.length; x++) {
					bc.append('<li><a href="/r/' + i.year + '/' + h[x].type + '/' + h[x].cod + '">' + h[x].name + '</a></li>');
				}
				bc.append('<li class="active">' + info.name + '</li>');
			}

			var hurl = "/h/" + i.type + "/" + i.cod;
			var curl = "/c?y=" + i.year + "&c=" + i.cod + "&t=" + i.type + "&n=" + encodeURIComponent(info.name ? info.name : "?");
			var gurl = "/g/" + i.year + "/" + i.type + "/" + i.cod;

			var hb = document.getElementById("headerbtn");
			hb.innerHTML =
				  '<div class="btn-group-vertical">'
				+ '	<a class="btn btn-default" onclick="if(slideout.isOpen()) { slideout.close(); } else { slideout.o=true; slideout.open(); }" data-title="Filtros"><span class="glyphicon glyphicon-filter"></span></a>'
				+ '	<a class="btn btn-default" href="' + hurl + '" data-title="Despesas Históricas"><span class="glyphicon glyphicon-stats"></span></a>'
				+ '	<a class="btn btn-default" href="' + curl + '" data-title="Comparar"><span class="glyphicon glyphicon-sort"></span></a>'
				+ '	<a class="btn ' + (hierarchy ? 'btn-success' : 'btn-danger') + '" onclick="toggleHierarchy(this);" data-title="Seguir Hierarquia ' + (hierarchy ? '✓' : '✖') + '"><span class="glyphicon glyphicon-sort-by-attributes"></span></a>'
				+ '</div>';
		}
	});

	if (i.req == "r" || i.req == "i" || i.req == "a") setSearchyear(i.year);
	else setSearchyear("2000");
	
	addTypeahead($("#search"), true);

	slideout = new Slideout({
		panel: document.getElementById("main"),
		menu: document.getElementById('slide'),
		padding: 256,
		tolerance: 70
	});
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
	if (data == null) data = /*$.extend(true, {}, */datacache[id]/*)*/;
	else datacache[id] = /*$.extend(true, {}, */data/*)*/;

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
	return d3.layout.pack()
		.sort(null)
		.size([div.offsetWidth, div.offsetHeight])
		.padding(1.5);
}

function reloadBubbles(id, color, uoDiv_, uoDiv, data, bubble) {
	var w = uoDiv_.offsetWidth;
	var h = uoDiv_.offsetHeight;
	
	var zoom = d3.behavior.zoom()
		.scaleExtent([1, 10])
		.on("zoom", redraw);

	function redraw() {
		var t = d3.event.translate,
		      s = d3.event.scale;
		  //t[0] = Math.min(w / 2 * (s - 1), Math.max(w / 2 * (1 - s), t[0]));
		  t[0] = Math.min(0, Math.max(2*(-(w/2*(s-1))), t[0]));
		  t[1] = Math.min(h / 2 * (s - 1) * s, Math.max(h / 2 * (1 - s) * s, t[1]));
		  zoom.translate(t);

		vis.attr('transform', "translate(" + t + ")" + " scale(" + s + ")");
	}

	if (data == null) data = /*$.extend(true, {}, */datacache[id]/*)*/;
	else datacache[id] = /*$.extend(true, {}, */data/*)*/;

	uoDiv_.innerHTML = "";

	var key;
	var s = document.getElementById(id + "-group").children;
	for (var i=0; i<s.length; i++) {
		if (s[i].classList.contains("active")) {
			key = s[i].getAttribute("data-key");
			break;
		}
	}
	
	var vis = uoDiv.append("svg")
		.attr("width", w)
		.attr("height", h)
		.attr("pointer-events", "all")
		.style("height", h)
		.attr("class", "bubble")
		.call(zoom)
		.append('g').attr('class', 'group2');
	
	var x = vis
		.selectAll(".nd")
		.data(bubble.value(function(d) { return d[key]; }).nodes(classes(data)).filter(function(d) { return !d.children; }))
		.enter().append("g")
		.attr("class", "nd")
		.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
	
	x.append("title").text(function(d) { return d.name; })
	x.append("circle")
		.attr("r", function(d){return d.r;})
		.style("fill", function(d){return color(d.packageName);})
		.attr("cursor", "pointer");
	x.append("text")
		.attr("cursor", "pointer")
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
	var h = JSON.parse(decodeURIComponent(getURLParameter("f")));
	if (h != null) {
		for (var i=0; i<h.length; i++) {
			if (h[i].type == type) return;
		}
	}

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

					if (hierarchy) {
						var obj = [];
						if (getURLParameter("f")) obj = JSON.parse(decodeURIComponent(getURLParameter("f")));
						obj.push({name: 'x', type: i.type, cod: i.cod});

						wl += "?f=" + encodeURIComponent(JSON.stringify(obj)) + "&h=1";
					}
					window.location = wl;
				}
			}

			var preloadGraph = graphs[g][0];
			var reloadGraph = graphs[g][1];
			reloadGraph(id, color, uoDiv_, uoDiv, null, preloadGraph(uoDiv_)).on('click', click);
		}

	});
	
	createFilter(id, tit);
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
		var columns = ["Plano Orçamentário", "Modalidade de Aplicação", "Elemento de Despesa", "Fonte de Recursos", "Valor"];
		
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

var reloaded = [];
function reloadDataG(id, type, g, fi) {
	var h = JSON.parse(decodeURIComponent(getURLParameter("f")));
	if (h != null) {
		for (var i=0; i<h.length; i++) {
			if (h[i].type == type) return;
		}
	}

	if (!fi) reloaded.push({'id': id, 'type': type});

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
		if (type == "Subtitulo") url += "?p=" + programa;
		if (getURLParameter("f")) url += (url.indexOf("?")>-1 ? "&" : "?") + "f=" + getURLParameter("f");
		
		if (!$.isEmptyObject(filter)) {
			url += (url.indexOf("?")>-1 ? "&" : "?") + "xf=" + encodeURIComponent(JSON.stringify(filter));
		}
	}
	
	var i = urlinfo();
	var click = function(d) {
		var i = urlinfo();
		if (type == "Subtitulo") window.location = "/i/" + i.year + "/" + programa + "/" + i.cod + "/" + d.cod;
		else {
			var wl = "/r/" + i.year + "/" + type + "/" + d.cod;

			if (hierarchy) {
				var obj = [];
				if (getURLParameter("f")) obj = JSON.parse(decodeURIComponent(getURLParameter("f")));
				obj.push({name: info.name, type: i.type, cod: i.cod});

				wl += "?f=" + encodeURIComponent(JSON.stringify(obj)) + "&h=1";
			}
			window.location = wl;
		}
	}

	d3.json(url, function(error, root) {
		var uoNodes = reloadGraph(id, color, uoDiv_, uoDiv, root, uoTreemap).on('click', click);

		if (!fi) reloadFilter(id, type, root);
		
		var columns = ["name", "size"];
		uoThead.append('tr')
			.selectAll('th')
			.data(columns)
			.enter().append('th')
			.text(function(column) { return column.localeCompare("name")==0 ? "Nome" : "Valor (R$)"; });

		var rows = uoTbody.selectAll('tr')
			.data(classes(root).children)
			.enter().append('tr')
			.style('cursor', 'pointer')
			.on('click', function(d) {
				window.location = "/r/" + i.year + "/" + type + "/" + d.cod;
			});
		var cells = rows.selectAll('td')
			.data(function(row) {
				return columns.map(function(column) {
					return {column: column, value: row[column], cod: row.cod};
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
							return {column: column, value: row[column], cod: row.cod};
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
		
		getJSON(documentURL() + "/UnidadeOrcamentaria" + (hierarchy ? "?f="+getURLParameter("f") : ""), function(info){
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
					if (btnkey == "noinf"){ return key=="loa" || key=="pago"; }
					return key=="infloa" || key=="infpago";
				});
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

/* Filters {{{ */

function createFilter(id, tit) {
	var slide = $("#slidecontent");
	slide.append('<h5 style="font-variant: small-caps; color: #757575; margin-left: 10px;">' + tit + '</p>');
	slide.append('<ul class="list-group" id="' + id + '-filter"></ul>');
	
}

function reloadFilter(id, type, data_) {
	var data = classes(data_).children;
	
	var filter = $("#slide").find("#" + id + "-filter");
	for (var i=0; i<data.length; i++) {
		var item = data[i];
		filter.append('<a class="list-group-item list-group-item-info rect" onclick="updateFilter(this, \'' + type + '\', \'' + item.cod + '\')">' + item.name + '</a>');
	}
}

function updateFilter(a, type, cod) {
	if (a.classList.contains("list-group-item-info")) {
		a.classList.remove("list-group-item-info");
		
		if (filter[type] == null) filter[type] = [];
		filter[type].push(cod);
	}
	else {
		a.classList.add("list-group-item-info");
		
		var arr = filter[type];
		arr.splice(arr.indexOf(cod), 1);
		if (arr.length < 1) delete filter[type];
	}
}

function applyFilter() {
	for (var i=0; i<reloaded.length; i++) {
		reloadDataG(reloaded[i].id, reloaded[i].type, 'sq', true);
	}
}

/* }}} */

/* Hierarchy {{{ */

function toggleHierarchy(a) {
	if (a.classList.contains("btn-danger")) {
		a.classList.remove("btn-danger");
		a.classList.add("btn-success");
		a.setAttribute("data-title", "Seguir Hierarquia ✓");
		hierarchy = true;
	}
	else {
		a.classList.remove("btn-success");
		a.classList.add("btn-danger");
		a.setAttribute("data-title", "Seguir Hierarquia ✖");
		hierarchy = false;
	}
}

/* }}} */
