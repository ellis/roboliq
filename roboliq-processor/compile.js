/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

const Metalsmith = require('metalsmith');

const babelOptions = {
  presets: ['es2015']
};

const path = require('path');
const babel = require('babel-core');
const toFastProperties = require('to-fast-properties');

function metalsmithBabel(options) {
  options = Object.assign({}, options);

  return function metalsmithBabelPlugin(files, metalsmith) {
    Object.keys(files).forEach(originalFilename => {
      const ext = path.extname(originalFilename).toLowerCase();
      if (ext !== '.js' && ext !== '.jsx') {
        return;
      }

      const filename = originalFilename.replace(/\.jsx$/i, '.js');

      if (originalFilename !== filename) {
        files[filename] = files[originalFilename];
        delete files[originalFilename];
        toFastProperties(files);
      }

      const result = babel.transform(String(files[filename].contents), Object.assign({}, options, {
        filename: path.join(metalsmith.directory(), metalsmith.source(), originalFilename),
        filenameRelative: originalFilename,
        sourceMapTarget: filename
      }));

      if (result.map) {
        const sourcemapPath = `${filename}.map`;
        files[sourcemapPath] = {
          contents: new Buffer(JSON.stringify(result.map))
        };

        // https://github.com/babel/babel/blob/v6.14.0/packages/babel-core/src/transformation/file/options/config.js#L123
        if (options.sourceMap !== 'both' && options.sourceMaps !== 'both') {
          result.code += `\n//# sourceMappingURL=${
            path.relative(path.dirname(filename), sourcemapPath).replace(/\\/g, '/')
          }\n`;
        }
      }

      files[filename].contents = new Buffer(result.code);
    });
  };
}


new Metalsmith(__dirname)
.source("src")
.destination("dist")
.use(metalsmithBabel(babelOptions))
.build(function (err, files) {
  if (err) {
    throw err;
  }

  console.log('Completed.');
});
