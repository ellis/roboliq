const _ = require('lodash');
const assert = require('assert');
const fs = require('fs');
const jsonfile = require('jsonfile');
const path = require('path');
const process = require('process');
const yaml = require('yamljs');

const electron = require('electron')
// Module to control application life.
const app = electron.app
// Module to create native browser window.
const BrowserWindow = electron.BrowserWindow

// Keep a global reference to the command line options
let opts;
// Keep a global reference of the window object, if you don't, the window will
// be closed automatically when the JavaScript object is garbage collected.
let mainWindow;

const commander = require('commander')
	.version("1.0")
	.option("--pmain [protocol]", "main protocol to load")
	.option("--poutput [protocol]", "output protocol to load")
	// .option("-d, --debug", "enable debugging output")
	// .option("-o, --output", "full path (directory and filename) to save the script to")
	// .option("-O, --outputDir", "directory to save the script to (defaults to the same directory as the input protocol)")
	// .option("-b, --outputBasename", "filename for the script (without directory) (defaults to basename of the input protocol)")
	// .option("--SCRIPTDIR [dir]", "value of SCRIPTDIR variable (default to directory where script is saved)")
	// .option("--progress", "display progress while compiling the script")
	// .arguments("[carrier] [table] [protocol] [agents]")
	// .description(
	// 	"Arguments:\n"+
	// 	"    carrier   path to Carrier.cfg\n"+
	// 	"    table     path to table file (.ewt or .esc)\n"+
	// 	"    protocol  path to compiled protocol (.out.json)\n"+
	// 	"    agents    list of agents to compile for (comma-separated)\n"
	// );

function createWindow () {
  // Create the browser window.
  mainWindow = new BrowserWindow({width: 800, height: 600})

  // and load the index.html of the app.
  mainWindow.loadURL(`file://${__dirname}/index.html`)

  // Open the DevTools.
  // mainWindow.webContents.openDevTools()

  // Emitted when the window is closed.
  mainWindow.on('closed', function () {
    // Dereference the window object, usually you would store windows
    // in an array if your app supports multi windows, this is the time
    // when you should delete the corresponding element.
    mainWindow = null
  });

	require('./menu/mainmenu');
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', () => {
	// Parse command line arguments
	opts = commander.parse(process.argv);
	if (opts.rawArgs.indexOf("--help") >= 0 || opts.rawArgs.indexOf("-h") >= 0) {
		opts.outputHelp();
		process.exit();
	}

	createWindow();
});

// Quit when all windows are closed.
app.on('window-all-closed', function () {
  // On OS X it is common for applications and their menu bar
  // to stay active until the user quits explicitly with Cmd + Q
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', function () {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (mainWindow === null) {
    createWindow();
  }
})

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.

const ipc = require('electron').ipcMain

ipc.on("loadCommandLineProtocols", (event) => {
	const filenames = {
		main: opts.pmain,
		output: opts.poutput
	};
	const result = loadProtocols(filenames);
	event.sender.send("loadProtocols", result);
});

ipc.on("loadProtocols", (event, filenames) => {
	const result = loadProtocols(filenames);
	event.sender.send("loadProtocols", result);
});

function loadProtocols(filenames) {
	console.log("filenames: "+JSON.stringify(filenames));
	const result = {filenames: {}, protocols: {}};
	const mainFilename = filenames.main;
	const outputFilename = filenames.output;

	function load(label, url) {
		let content;
		if (path.extname(url) === ".yaml")
			content = yaml.load(url);
		else if (path.extname(url) === ".json")
			content = jsonfile.readFileSync(url);
		result.filenames[label] = url;
		result.protocols[label] = content;
	}

	if (mainFilename) load("main", mainFilename);
	if (outputFilename) load("output", outputFilename);

	return result;
}
