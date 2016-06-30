var datasets = {
  "Despesas Federais": "federal",
  "Despesas Estado de São Paulo": "estadual",
  "Despesas Municípios de São Paulo": "municipal",
  "Despesas Município de São Paulo": "capital",
  "Receitas Federais": "federalr",
  "Receitas Estado de São Paulo": "estadualr",
  "Receitas Municípios de São Paulo": "municipalr"
}

var searchyear;
var searchset;

function setSearchyear(s) {
	searchyear = s;
	$("#ddyear").attr("value", searchyear);
}

function setSearchset(s) {
  searchset = s;
  $("#ddset").attr("value", searchset);
}

function addTypeahead(el, isSearch, pfn, tfn) {
	var timeout;
	var f = function(q, cb) {
		if (timeout) clearTimeout(timeout);
		timeout = setTimeout(function() {
			$.post("/s", {year: el.find('.yearmenu').attr('data-sel'), set: el.find('.setmenu').attr('data-sel'), query: q, count: 6}, function(data){
				cb(JSON.parse(data));
			});
		}, 500);
	};	
	
	el.find("input[type='text']").typeahead({
		hint: true,
		highlight: true,
		minLength: 3
	}, {
		name: 'resources',
		displayKey: 'value',
		source: f,
		templates: {
			suggestion: function(data) {
				return '<div style="padding: 10px;">'
				+ '	<p style="font-size: 16pt; display: table-row;">' + data.value + '</p>'
				+ '	<p style="font-size: 14pt; display: table-row; color: #757575;">' + translatetype[data.type] + ' – ' + data.codigo + '</p>'
				+ '</div>';
			},
			empty: '<div style="padding: 10px;"><p style="font-size: 16pt; display: table-row;">Não há resultados disponíveis para esta pesquisa.</p></div>'
		}
	}).on("typeahead:selected", function(obj, data, name) {
		if (data.type == 'placeholder') return;

		if (isSearch) window.location = "/r/" + searchset + "/" + searchyear + "/" + data.type + "/" + data.codigo;
		else pfn(this, data);
	});

	// Hack: Get correct size for text bar.
	el.find(".tt-hint").each(function() { $(this).css("width", "100%"); });
	el.find(".tt-input").each(function() { $(this).css("width", "100%"); });
	
	// FIXME
	var dd = el.find(".yearmenu");
	for (var i=2000; i<2017; i++) {
		dd.append('<li><a>' + i + '</a></li>');
	}

  var ee = el.find(".setmenu");
  for (var i in datasets) {
    ee.append('<li><a>' + i + '</a></li>');
  }

	if (isSearch) {
		el.find(".yearbtn").html(searchyear + ' <span class="caret"></span>');
		dd.attr('data-sel', searchyear);
		
    // FIXME this is likely the biggest gambiarra ive done in 5 years of programming
    // i need to finish this i wanna go play civilization v
    // i swear i dont do things like this at work
    var x
    for (x in datasets) {
      if (datasets[x] == searchset) {
        break
      }
    }

    el.find(".setbtn").html(x + ' <span class="caret"></span>');
		ee.attr('data-sel', searchset);
	}
	el.find('.dropdown-menu li a').on('click', function(){
		var dropdown = $(this).parent().parent();
		if (isSearch) {
      if (dropdown.hasClass('yearmenu')) setSearchyear($(this).text());
      else if (dropdown.hasClass('setmenu')) setSearchset(datasets[$(this).text()])
    }
		
		dropdown.siblings('button').html($(this).text() + ' <span class="caret"></span>');
		dropdown.attr("data-sel", dropdown.hasClass('yearmenu') ? $(this).text() : datasets[$(this).text()]);

		if (tfn != null) tfn(dropdown, $(this).text());
	});
}
