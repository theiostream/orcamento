var translatetype = {
	"Orgao": "Órgão",
	"UnidadeOrcamentaria": "Unidade Orçamentária",
	"Funcao": "Função",
	"Subfuncao": "Subfunção",
	"Programa": "Programa",
	"Acao": "Ação"
};

function getURLParameter(name) {
  return /*decodeURIComponent(*/(new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search)||[,""])[1].replace(/\+/g, '%20')/*)*/||null;
}

var i1 = null;
var i2 = null;

function init() {
	setSearchyear("2000");
	addTypeahead($("#search"), true);
	
	var pfn = function(e, data) {
		if ($(e).attr('id') == 'ip1') i1 = data;
		else i2 = data;
		
		if (i1 && i2)
			showdiff($("#item1").find('.yearmenu').attr('data-sel'), i1.type, i1.codigo, $("#item2").find('.yearmenu').attr('data-sel'), i2.type, i2.codigo);
	};
	addTypeahead($("#item1"), false, pfn);
	addTypeahead($("#item2"), false, pfn);

	showdiff("2013", "Orgao", "26000", "2013", "Orgao", "36000");
}

function showdiff(year1, type1, codigo1, year2, type2, codigo2) {
	$("#graph").empty();

	$.post("/cp", {y1: year1, t1: type1, c1: codigo1, y2: year2, t2: type2, c2: codigo2}, function(cdata) {
		var color = function(c) {
			return c==0 ? "#aa0114" : "#21b6a8";
		}

		var data = JSON.parse(cdata);
		console.log(data);
		
		var margin = {top:20, right:20, bottom:50, left:40};
		var width = $("#graph").width() - margin.left - margin.right;
		var height = $("#graph").height() - margin.top - margin.bottom;
		
		var x = d3.scale.ordinal().rangeRoundBands([0, width]);
		var y = d3.scale.linear().range([height, 0]);

		var xAxis = d3.svg.axis()
			.scale(x)
			.orient("bottom");
		var yAxis = d3.svg.axis()
			.scale(y)
			.orient("left")
			.tickFormat(d3.format(".2s"));

		var svg = d3.select("#graph").append("svg")
			.attr("width", width + margin.left + margin.right)
			.attr("height", height + margin.top + margin.bottom)
			.append("g")
			.attr("transform", "translate(" + margin.left + "," + margin.bottom + ")");

		x.domain(data.map(function(d) { return d.res; }));
		y.domain([0, d3.max(data, function(d) { return d.value; })]);

		svg.append("g")
			.attr("class", "x axis")
			.attr("transform", "translate(0," + height + ")")
			.call(xAxis);
		svg.append("g")
			.attr("class", "y axis")
			.call(yAxis)
			.append("text")
			.attr("transform", "rotate(-90)")
			.attr("dy", ".71em")
			.style("text-anchor", "end")
			.text("Valor");

		svg.selectAll("rect")
			.data(data)
			.enter().append("rect")
			.attr("width", x.rangeBand())
			.attr("x", function(d) { return x(d.res); })
			.attr("y", function(d) { return y(d.value); })
			.attr("height", function(d) { return height - y(d.value); })
			.style("fill", function(d) { return color(d.c); });
		
		var b, s;
		if (data[0].value > data[1].value) { b=data[0]; s=data[1]; }
		else { b=data[1]; s=data[0]; }
		
		var times = (b.value / s.value).toFixed(2);
		var pc = s.value / b.value;
		var pi = (b.value - s.value)/s.value;

		$("#text").html('<p>' + s.res + ' caberia <b>' + times + '</b> vezes dentro de ' + b.res + '</p>'
				+ '<p>O que foi gasto em ' + s.res + ' equivale a <b>' + (pc*100).toFixed(2) + '%</b> do que foi gasto em ' + b.res + '</p>'
				+ '<p>Um aumento de <b>' + (pi*100).toFixed(2) + '%</b> nos gastos em ' + s.res + ' resultaria no que foi gasto em ' + b.res + '</p>'); 
	});
}
