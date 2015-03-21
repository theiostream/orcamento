var searchyear;
function setSearchyear(s) {
	searchyear = s;
}

function addTypeahead(el) {
	var timeout;
	var f = function(q, cb) {
		if (timeout) clearTimeout(timeout);
		timeout = setTimeout(function() {
			$.post("/s", {year: searchyear, query: q, count: 6}, function(data){
				cb(JSON.parse(data));
			});
		}, 500);
	};	
	
	el.typeahead({
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
		console.log("SELECT");
		window.location = "/r/" + searchyear + "/" + data.type + "/" + data.codigo;
	});

	// Hack: Get correct size for text bar.
	$(".tt-hint").each(function() { $(this).css("width", "100%"); });
	$(".tt-input").each(function() { $(this).css("width", "100%"); });
	
	// FIXME
	var dd = $("#ddmenu");
	for (var i=2000; i<2015; i++) {
		dd.append('<li><a href="#">' + i + '</a></li>');
	}

	$("#ddbtn:first-child").html(searchyear + ' <span class="caret"></span>');
	$("#ddyear").attr("value", searchyear);

	$('#ddmenu li a').on('click', function(){
		searchyear = $(this).text();
		$("#ddbtn:first-child").html(searchyear + ' <span class="caret"></span>');
		$("#ddyear").attr("value", searchyear);
	});

	// Hack: Get <Enter> to go submit form instead of going back to the year selector.
	$("#search").keypress(function(e) {
		if (e.which == 10 || e.which == 13) this.form.submit();
	});	
}
