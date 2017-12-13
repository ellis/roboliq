/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017, ETH Zurich, Ellis Whitehead
 * @license GPL-3.0
 */

require("babel-register");
var designRun = require('./designRun.js');

designRun.run(process.argv);
