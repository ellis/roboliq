var fs = require('fs');
var path = require('path');

var date = new Date();

var action;
var begins = [];
var ends = [];

for (var i = 2; i < process.argv.length; i++) {
  var step = process.argv[i];
  if (step === "begin" || step === "end") {
    action = step;
  }
  else {
    if (action === "begin") {
      begins.push(step);
    }
    else if (action === "end") {
      ends.push(step);
    }
    else {
      console.log("ERROR: step given without 'begin' or 'end' command")
    }
  }
}

if (begins.length > 0) {
  console.log({type: "stepTimeBegin", time: date, steps: begins});
}
if (ends.length > 0) {
  console.log({type: "stepTimeEnd", time: date, steps: ends});
}
