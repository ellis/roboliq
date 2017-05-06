const _forEach = require('lodash/forEach');
const _isArray = require('lodash/isArray');
const _isPlainObject = require('lodash/isPlainObject');
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

function processSchemas(x) {
	// console.log("processSchemas:")
	if (_isPlainObject(x)) {
		if (x.hasOwnProperty("properties") && _isPlainObject(x.properties)) {
			// console.log("hasProperties")
			// If properties are required, add `required=true` to them
			if (x.hasOwnProperty("required") && _isArray(x.required)) {
				x.required.forEach(function(name) {
					// console.log("required: "+name)
					if (x.properties.hasOwnProperty(name)) {
						x.properties[name].required = "required";
					}
				})
			}
			// Add some defaults
			if (x.properties) {
				if (x.properties.description && !x.properties.description.description)
					x.properties.description.description = "Optional user description of this item";
				if (x.properties.label && !x.properties.label.description)
					x.properties.label.description = "Optional user label for this item";
				if (x.properties.type && !x.properties.type.description)
					x.properties.type.description = "Type of this object";
				if (x.properties.type && !x.properties.type.type)
					x.properties.type.type = "string";
				// Recurse into properties
				_forEach(x.properties, function(value, key) {
					if (!value.type && value.enum)
						value.type = "string";
					processSchemas(value);
				});
			}
		}
		else {
			_forEach(x, function(value, key) {
				processSchemas(value);
			});
		}
	}
}

metalsmith(__dirname)
	.source("content")
	//.source(__dirname+"/../src/schemas")
	.destination("../docs/protocol")
	.use(collections({
		commands: {
			pattern: "schemas/commands/*.yaml"
		},
		evowareConfiguration: {
			pattern: "schemas/evowareConfiguration/*.yaml"
		}
	}))
	.use(function (files, metalsmith, done) {
		_forEach(files, function(file, filename) {
			if (path.extname(filename).toLowerCase() === ".yaml") {
				file.data = YAML.parse(file.contents.toString("utf8"));
				processSchemas(file.data);
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
