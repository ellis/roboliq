yaml = require('js-yaml');
fs   = require('fs');

// Get document, or throw exception on error
try {
  var doc = yaml.safeLoad(fs.readFileSync('protocol1.yaml', 'utf8'));
  console.log(JSON.stringify(doc));
} catch (e) {
  console.log(e);
}
