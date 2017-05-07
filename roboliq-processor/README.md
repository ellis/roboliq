# Roboliq Processor

Roboliq takes biological protocols and compiles them for robotic lab automation.
This documentation was generated with [JSDoc](http://usejsdoc.org).
It contains programmer documentation about the functions used in Roboliq's
protocol processor.

# Additional documentation
* [Manual](../manual/index.html) -- the Roboliq manual
* [Processor API](../roboliq-processor/index.html) -- programmer documentation for Roboliq's protocol processor
* [Evoware API](../roboliq-evoware/index.html) -- programmer documentation for Roboliq's Evoware backend


# Setup

```{sh}
npm install
```

# Development

To run the tests:

```{sh}
npm test
```

To update the parsers (e.g for well locations):

```{sh}
npm run pegjs
```

To generate the HTML documentation:

```{sh}
npm run jsdoc
```
