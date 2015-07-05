start
  = well
  / source

well
  = labware:primary '(' wellId:wellId ')' { return {labware: labware, wellId: wellId}; }

source
  = source:primary { return {source: source}; }

// TODO: The identifier should not end with a '.'
primary
  = first:[A-Za-z_] rest:[0-9A-Za-z._]* { return first.toString() + rest.join('').toString(); }

// FIXME: need to format leading zeros, but the code below won't allow for columns >99
wellId
  = row:[A-Z] col:column { return ""+row+(("0" + col).slice(-2)); }

column "column"
  = digits:[0-9]+ { return parseInt(digits.join(""), 10); }
