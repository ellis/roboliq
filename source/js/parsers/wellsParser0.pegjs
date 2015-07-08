/*
 * labware1(subject1 phrase1*, subject2 phrase2*) + labware2(...)
 *
 * location: A01
 * locationClause: subject1 phrase1*
 * labwareClause: labware(locationClause*)
 * entity: labwareClause | source
 * start: entity*
 */

start
  = init:(x:entity ws '+' ws { return x; })* last:entity{ return init.concat.apply([], init).concat(last); }

ws = [ \t]*

spaces = [ \t]+

entity
  = labwareClause
  / source

labwareClause
  = labware:ident ws '(' ws clauses:locationClauses ws ')' { return clauses.map(function (clause) { return {labware: labware, subject: clause.subject, phrases: clause.phrases}; }); }

source
  = source:ident { return {source: source}; }

ident
  = init:(x:identPart ws '.' ws { return x; })* last:identPart { return init.concat([last]).join('.'); }

identPart
  = first:[A-Za-z_] rest:[0-9A-Za-z_]* { return first.toString() + rest.join('').toString(); }
  / [A-Za-z_]

locationClauses
  = init:(x:locationClause ws ',' ws { return x; })* last:locationClause { return init.concat([last]); }

locationClause
  = subject:locationSubject ws phrases:locationPhrases?
  { return {subject: subject, phrases: phrases}; }

locationSubject
  = location
  / "all"

// Location on labware; matches strings such as "A01"
location
  = row:[A-Z] col:integer
  {
    var columnText = ("0" + col);
    if (columnText.length > 2)
      columnText = columnText.substr(1);
    return row.toString()+columnText;
  }

integer
  = digits:[0-9]+ { return parseInt(digits.join(""), 10); }

locationPhrases
  = init:(x:locationPhrase spaces { return x; })* last:locationPhrase { return init.concat([last]); }

locationPhrase
  = 'down' ws n:integer { return ["down", n]; }
  / 'down' ws to:location { return ["down-to", to]; }
  / 'right' ws n:integer { return ["right", n]; }
  / 'right' ws to:location { return ["right-to", to]; }
  / 'block' ws to:location { return ["block-to", to]; }
  / 'random' ws '(' ws seed:integer ws ')' { return ['random', seed]; }
  / 'random' { return ['random']; }
  / 'take' ws n:integer { return ['take', n]; }
