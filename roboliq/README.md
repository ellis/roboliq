# Roboliq

Roboliq compiles biological protocols specifications down to low-level commands
for robotic lab automation.

# Setup

```{sh}
npm install
```

# Usage

To run a protocol:

```{sh}
./node_modules/.bin/babel-node -- src/main.js --help
./node_modules/.bin/babel-node -- src/main.js [options] ${PROTOCOL}
```

# Development

To run the tests:

```{sh}
npm test
```

To generate the documentation:

```{sh}
npm run jsdoc
```

To update the parser for well locations:

```{sh}
npm run pegjs
```
