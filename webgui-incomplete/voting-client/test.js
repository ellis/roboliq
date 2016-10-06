var YAML = require('js-yaml');
var fs = require('fs');

var l = [];
YAML.safeLoadAll(fs.readFileSync("/home/ellis/repo/quvault/problemSets/wharton-accounting.yaml", "utf-8"), doc => {l.push(doc);})
