const jsdom = require('jsdom');
const chai = require('chai');
const chaiImmutable = require('chai-immutable');

const doc = jsdom.jsdom('<!doctype html><html><body></body></html>');
const win = doc.defaultView;

global.document = doc;
global.window = win;

// from mocha-jsdom https://github.com/rstacruz/mocha-jsdom/blob/master/index.js#L80
Object.keys(window).forEach((key) => {
  if (!(key in global)) {
    global[key] = window[key];
  }
});


chai.use(chaiImmutable);
