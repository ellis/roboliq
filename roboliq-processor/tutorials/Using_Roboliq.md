This document contains information for users who want to run Roboliq.

## Preparation

1. First, please install [Node.js](https://nodejs.org/), which provides
   the Node Pack Manager (`npm`).
2. Obtain Roboliq's source code.
3. Change to the directory containing Roboliq's source code and run
   execute this from the command line:
   ```
   npm install
   ```

## Running Roboliq

To run Roboliq on a protocol, use a command similar to the following:

```{sh}
npm run roboliq -- [options] INFILES
```

where `[options]` are command line options, and `INFILES` are one or more
input files (e.g. protocols).  The available options are as follows:

```
Options:
   -d, --debug                Print debugging info
   --file-data                Supply filedata on the command line in the form of 'filename:filedata'
   --file-json                Supply a JSON file on the command line in the form of 'filename:filedata'
   --ourlab                   automatically load config/ourlab.js  [true]
   -o FILE, --output FILE     specify output filename or "" for none; otherwise the default filename is used
   -O DIR, --output-dir DIR   specify output directory
   -p, --print                print output
   -r, --print-protocol       print combined protocol
   --progress                 print progress indicator while processing the protocol
   -T, --throw                throw error when errors encountered during processing (in order to get a backtrace)
   --version                  print version and exit
```

Some sample protocols are provided in the `protocols` subdirectory.
For example, to process `protocol2.json`, you could use this command:

```{sh}
npm run roboliq -- protocols/protocol2.json
```
