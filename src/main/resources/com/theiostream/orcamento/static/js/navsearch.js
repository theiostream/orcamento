var searchyear;
function setSearchyear(s) {
	searchyear = s;
	$("#ddyear").attr("value", searchyear);
}

function addTypeahead(el, isSearch, pfn, tfn) {
	var timeout;
	var f = function(q, cb) {
		if (timeout) clearTimeout(timeout);
		timeout = setTimeout(function() {
			$.post("/s", {year: el.find('.yearmenu').attr('data-sel'), query: q, count: 6}, function(data){
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
			}
		}
	}).on("typeahead:selected", function(obj, data, name) {
		if (isSearch) window.location = "/r/" + searchyear + "/" + data.type + "/" + data.codigo;
		else pfn(this, data);
	});

	// Hack: Get correct size for text bar.
	el.find(".tt-hint").each(function() { $(this).css("width", "100%"); });
	el.find(".tt-input").each(function() { $(this).css("width", "100%"); });
	
	// FIXME
	var dd = el.find(".yearmenu");
	for (var i=2000; i<2015; i++) {
		dd.append('<li><a>' + i + '</a></li>');
	}

	if (isSearch) {
		el.find(".yearbtn").html(searchyear + ' <span class="caret"></span>');
		dd.attr('data-sel', searchyear);
	}
	el.find('.dropdown-menu li a').on('click', function(){
		if (isSearch) setSearchyear($(this).text()); /* danger: this assumes the only dropdown in isSearch cases is the year one */
		
		var dropdown = $(this).parent().parent();
		dropdown.siblings('button').html($(this).text() + ' <span class="caret"></span>');
		dropdown.attr("data-sel", $(this).text());

		if (tfn != null) tfn(dropdown, $(this).text());
	});
}
