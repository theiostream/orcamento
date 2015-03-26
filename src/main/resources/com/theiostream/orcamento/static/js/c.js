function urlinfo() {
	var s = location.pathname.split('/');
	return {
		req: s[1],
		year: s[2],
		type: s[3],
		cod: s[4]
	};
}

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

var i1 = {};
var i2 = {};

function red(el) {
	console.log("Input!");

	el.parentElement.parentElement.classList.add("has-error");
	el.parentElement.parentElement.classList.remove("has-success");
}

function init() {
	setSearchyear("2000");
	addTypeahead($("#search"), true);
	
	if (getURLParameter("y") && getURLParameter("c") && getURLParameter("t")) {
		i1 = {
			year: getURLParameter("y"),
			type: getURLParameter("t"),
			codigo: getURLParameter("c")
		};

		$.get("/r/" + i1.year + "/" + i1.type + "/" + i1.codigo + "/i", function(data) {
			console.log(data);
			$('#item1').find('.yearbtn').html(i1.year + ' <span class="caret"></span>');
			$("#item1").find('.yearmenu').attr('data-sel', i1.year);
			$('#ip1').val(data.name);
			document.getElementById('item1').classList.add('has-success');
		});
	}

	var pfn = function(e, data) {
		console.log("Pfn!");
		$(e).trigger({ type: 'keypress', which: 9 }); //FIXME

		e.parentElement.parentElement.classList.remove("has-error");
		e.parentElement.parentElement.classList.add("has-success");

		if ($(e).attr('id') == 'ip1') {
			for (var k in data) i1[k] = data[k];
			i1.year = $("#item1").find('.yearmenu').attr('data-sel');
		}
		else {
			for (var k in data) i2[k] = data[k];
			i2.year = $("#item2").find('.yearmenu').attr('data-sel');
		}
		
		if (i1.value && i2.value) {
			$("#tit").text("Comparação de Valores");
			showdiff();
		}
		else if (i1.value || i2.value)
			$("#tit").text("Selecione um item para comparar...");
	};

	var tfn = function(e, sel) {
		var id = e.parent().parent().attr('id');

		if (e.hasClass('yearmenu')) {
			if (id == 'item1') { $("#ip1").val(""); red(document.getElementById('ip1')); }
			else { $("#ip2").val(""); red(document.getElementById('ip2')); }

			$("#graph").html("");
			$("#text").html("");
		}
		else {
			if (id == 'item1') i1.v = sel;
			else i2.v = sel;
			
			if (i1.value && i2.value) showdiff();
		}

	}

	addTypeahead($("#item1"), false, pfn, tfn);
	addTypeahead($("#item2"), false, pfn, tfn);
}

function showdiff() {
	$("#graph").empty();
	
	var x = {y1: i1.year, t1: i1.type, c1: i1.codigo, v1: i1.v?i1.v:'LOA', y2: i2.year, t2: i2.type, c2: i2.codigo, v2: i2.v?i2.v:'LOA'};
	console.log(x);
	$.post("/cp", x, function(cdata) {
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
		
		$("#text").html('<p>' + s.res + ' caberia <b>' + times + '</b> vezes dentro de ' + b.res + '.</p>'
				+ '<p>O que foi gasto em ' + s.res + ' equivale a <b>' + (pc*100).toFixed(2) + '%</b> do que foi gasto em ' + b.res + '.</p>'
				+ '<p>Um aumento de <b>' + (pi*100).toFixed(2) + '%</b> nos gastos em ' + s.res + ' resultaria no que foi gasto em ' + b.res + '.</p>'
				+ '<p style="font-size: 10pt; color: #757575;">' + i1.value + ' é expresso em valor ' + (i1.v?i1.v:"LOA") + ' e se refere a ' + i1.year + '.<br>' + i2.value + ' é expresso em valor ' + (i2.v?i2.v:"LOA") + ' e se refere a ' + i2.year + '.</p>'
			); 
	});
}
