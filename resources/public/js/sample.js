// Function render adapted from Simple JavaScript Templating
// John Resig - http://ejohn.org/ - MIT Licensed        

function App () {
	this.dataSource = '';
	this.cache = {};
	this.render = function tmpl(template, data) {
		var fn = this.cache[template] ||
		new Function("obj",
					  "var p=[],print=function()"+
					  "{p.push.apply(p,arguments);};" +
					  "with(obj)     var data=arguments[0],"+
					  "messages=arguments[1];{p.push('" +
					 template
					 .replace(/[\r\t\n]/g, " ")
					 .split("<%").join("\t")
					 .replace(/((^|%>)[^\t]*)'/g, "$1\r") //'
							  .replace(/\t=(.*?)%>/g, "',$1,'")
							  .split("\t").join("');")
							  .split("%>").join("p.push('")
							  .split("\r").join("\\'")
							  + "');}return p.join('');");
					 if(!this.cache[template]) { this.cache[template] = fn; }
					 return data ? fn( data ) : fn;
					 };

		// container is css selector for container
		this.render_in = function(container, template, data) {
			$(container).html(this.render(template, data));
		};
}
$app = new App();

function doSearch (url)
{
	$app.dataSource = url;
	var gridTemplate = $("#grid-template").html();
	$.ajaxSetup({ cache: false, dataType: 'json' });
	$.get(url,		  
		  function(data) { $app.render_in('#results', gridTemplate, data); });
}

function newRecord ()
{
	var data = {};
	var formTemplate = $("#form-template").html();
	$app.render_in('#dialog', formTemplate, data); 
	$("#dialog").dialog({ modal: true, width: 600, height: 400 });
}


function submitForm()
{
	var form = $("#character-form");
    var postSubmitTemplate = $("#post-submit").html();
    $.ajaxSetup({ cache: false, dataType: 'json' });
    $.post(form.attr('action'),
		   form.serialize(),
		   function(data) { $app.render_in('#dialog', postSubmitTemplate, data); });
}

function refresh()
{
	$('#dialog').dialog('close');
	doSearch($app.dataSource);
}
