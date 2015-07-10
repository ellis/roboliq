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
  = row:[A-Z] col:column
    {
      var columnText = col.toString();
      if (columnText.length < 2) columnText = "0" + columnText;
      return row.toString()+columnText;
    }

column "column"
  = digits:[0-9]+ { return parseInt(digits.join(""), 10); }
