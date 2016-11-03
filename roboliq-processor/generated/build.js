const _forEach = require('lodash/forEach');
const path = require('path');
const YAML = require('yamljs');

const metalsmith = require('metalsmith');
const collections = require('metalsmith-collections');
const markdown = require('metalsmith-markdown');
const metallic = require('metalsmith-metallic'); // code syntax highlighting
const assets = require('metalsmith-assets');
const inplace = require('metalsmith-in-place');
const layouts = require('metalsmith-layouts');
const handlebars = require('handlebars');
const marked = require('marked');

const templateConfig = {
	engine: 'handlebars',
	directory: "layouts",
	partials: "partials",
	default: 'default.html',
	pattern: '**/*.html'
};

handlebars.registerHelper('md', function(text) {
	if (text) {
		var html = marked(text);
		return new handlebars.SafeString(html);
	}
	else {
		return "";
	}
});

metalsmith(__dirname)
	.source("content")
	//.source(__dirname+"/../src/schemas")
	.destination("dist")
	.use(collections({
		commands: {
			pattern: "schemas/*.yaml"
		}
	}))
	.use(function (files, metalsmith, done) {
		_forEach(files, function(file, filename) {
			if (path.extname(filename).toLowerCase() === ".yaml") {
				file.data = YAML.parse(file.contents.toString("utf8"));
			}
		});
		done();
	})
	.use(markdown({
		smartypants: true,
		smartLists: true,
		gfm: true,
		tables: true
	}))
	.use(metallic())
	.use(inplace(templateConfig))
	.use(layouts(templateConfig))
	.use(assets({source: "assets"}))
	.build(function(err) {
		if (err) { console.error(err); }
	});
