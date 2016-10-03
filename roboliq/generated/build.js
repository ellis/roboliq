const path = require('path');
const metalsmith = require('metalsmith');
const markdown = require('metalsmith-markdown');
const assets = require('metalsmith-assets');
const inplace = require('metalsmith-in-place');
const layouts = require('metalsmith-layouts');

const templateConfig = {
	engine: 'handlebars',
	directory: "layouts",
	partials: "partials",
	default: 'default.html',
	pattern: '**/*.html'
};

metalsmith(__dirname)
	.source("content")
	.destination("dist")
	.use(markdown())
	.use(inplace(templateConfig))
	.use(layouts(templateConfig))
	.use(assets({source: "assets"}))
	.build(function(err) {
		if (err) { console.error(err); }
	});
