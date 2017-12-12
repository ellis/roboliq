/**
 * Roboliq: Automation for liquid-handling robots
 * @copyright 2017 Ellis Whitehead
 * @license GPL-3.0
 */

module.exports = {
    entry: "./src/roboliq.js",
    output: {
        path: __dirname,
        filename: "bundle.js"
    },
    module: {
        loaders: [
            { test: /\.js$/, exclude: /node_modules/, loader: 'babel-loader', query: { presets: ['es2015'] } },
            { test: /\.json$/, loader: "json" },
            { test: /\.yaml$/, loader: "json!yaml"}
        ]
    },
    target: "node"
};
