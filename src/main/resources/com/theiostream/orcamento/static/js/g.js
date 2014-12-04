// g.js
// d3.js <-> Orçamento Data bridge
// (c) 2014 Daniel Ferreira

// MUST IMPORT D3.JS BEFORE IMPORTING THIS
// I DISLIKE JAVASCRIPT
// NO I AM NOT PUTTING JQUERY HERE

function createGraph(id, tit) {
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
		+ '	<div class="graphcontent">'
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

var nodes = [];
function reloadData(id, type) {
	var color = d3.scale.category20();

	var uoDiv_ = document.getElementById(id + "-treemap");
	var uoDiv = d3.select("#" + id + "-treemap");

	var uoTreemap = d3.layout.treemap()
		.size([uoDiv_.offsetWidth, uoDiv_.offsetHeight])
		.sticky(true)
		.value(function(d) { return d.size; });
	
	var uoTable = d3.select("#" + id + "-legenda");
	var uoThead = uoTable.append('thead');
	var uoTbody = uoTable.append('tbody');
	
	d3.json(document.URL + "/" + type, function(error, root) {
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
				window.location = "/r/" + type + "/" + d.cod;
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

