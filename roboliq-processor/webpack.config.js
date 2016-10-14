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
