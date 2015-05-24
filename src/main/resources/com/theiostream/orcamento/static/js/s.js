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

function init() {
	$("#tit").text('Resultados para "' + decodeURIComponent(getURLParameter("s")) + '"');

	setSearchyear(getURLParameter("y"));
	addTypeahead($("#search"), true);

	var f = $("#filter");
	var keys = Object.keys(translatetype);

	for (var i=0; i<keys.length; i++) {
		var k = keys[i];
		var a = 
			'<a class="list-group-item list-group-item-info" onclick="updateFilter(this, \'' + k + '\');">'
			+ '<span>' + translatetype[k] + '</span>'
			+ '<span class="pull-right"><span class="glyphicon glyphicon-check"></span></span>'
			+ '</a>';
		
		f.append(a);
	}
}

var cache = null;
var filter = {};
function reload() {
	if (cache == null) {
		$.post("/s", {year: getURLParameter("y"), query: getURLParameter("s"), count: 1000}, function(data){
			cache = JSON.parse(data);
			reload_(filter);
		});
	}
	else reload_(filter);
}

function reload_(filter) {
	var table = $("#datatable");
	table.empty();

	cache.forEach(function(d) {
		if (filter[d.type] == true) return;

		var style =
			'<a style="text-decoration: none;" href="/r/' + getURLParameter("y") + "/" + d.type + "/" + d.codigo + '">'
			+ '	<p class="lead" style="color: #0066CC;">' + d.value + '</p>'
			+ '	<p style="margin-top: -7px; font-size: 13pt; color: #757575;">' + translatetype[d.type] + ' – ' + d.codigo + '</p>'
			+ '</a>';

		table.append('<tr><td class="searchitem">' + style + "</td></tr>");
	});

	if (table.is(':empty')) {
		table.append('<tr><td><p class="lead">Não foram encontrados resultados.</p></td></tr>');
	}

	//$("tr:even").css("background-color", "#eeeeee");
}

function updateFilter(a, key) {
	if (a.classList.contains("list-group-item-info")) {
		filter[key] = true;
		a.classList.remove("list-group-item-info");

		$(a).children(".pull-right").html('<span class="glyphicon glyphicon-unchecked"></span>');
	}
	else {
		filter[key] = false;
		a.classList.add("list-group-item-info");
		
		$(a).children(".pull-right").html('<span class="glyphicon glyphicon-check"></span>');
	}

	reload_(filter);
}
