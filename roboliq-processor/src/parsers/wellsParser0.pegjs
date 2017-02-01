/*
 * labware1(subject1 phrase1*, subject2 phrase2*) + labware2(...)
 *
 * well: A01
 * wellClause: subject1 phrase1*
 * labwareClause: labware(wellClause*)
 * entity: labwareClause | source
 * start: entity*
 */

start
  = init:(x:entity ws '+' ws { return x; })* last:entity{ return init.concat.apply([], init).concat(last); }

startOne
  = labware:ident ws '(' ws wellId:well ws ')' { return {labware:labware, wellId: wellId}; }
  / source

ws = [ \t]*

spaces = [ \t]+

entity
  = labwareClause
  / wellClause
  / source

labwareClause
  = labware:ident ws '(' ws clauses:wellClauses ws ')' { return clauses.map(function (clause) { return {labware: labware, subject: clause.subject, phrases: clause.phrases}; }); }

source
  = source:ident { return {source: source}; }

ident
  = init:(x:identPart ws '.' ws { return x; })* last:identPart { return init.concat([last]).join('.'); }

identPart
  = first:[A-Za-z_] rest:[0-9A-Za-z_]* { return first.toString() + rest.join('').toString(); }
  / [A-Za-z_]

wellClauses
  = init:(x:wellClause ws ',' ws { return x; })* last:wellClause { return init.concat([last]); }

wellClause
  = subject:wellSubject phrases:wellPhrases?
  { return {subject: subject, phrases: phrases}; }

wellSubject
  = well
  / "all"

// well on labware; matches strings such as "A01"
well
  = row:[A-Z] col:integer
  {
    var columnText = col.toString();
    if (columnText.length < 2) columnText = "0" + columnText;
    return row.toString()+columnText;
  }

integer
  = digits:[0-9]+ { return parseInt(digits.join(""), 10); }

wellPhrases
  = phrases:(spaces x:wellPhrase { return x; })*

wellPhrase
  = 'down' spaces 'block' spaces ('to' spaces)? to:well { return ["down-block", to]; }
  / 'down' spaces ('take' spaces)? n:integer { return ["down", n]; }
  / 'down' spaces ('to' spaces)? to:well { return ["down-to", to]; }
  / 'right' spaces 'block' spaces ('to' spaces)? to:well { return ["right-block", to]; }
  / 'right' spaces ('take' spaces)? n:integer { return ["right", n]; }
  / 'right' spaces ('to' spaces)? to:well { return ["right-to", to]; }
  / 'random' ws '(' ws seed:integer ws ')' { return ['random', seed]; }
  / 'random' ws '(' ws ')' { return ['random']; }
  / 'take' spaces n:integer { return ['take', n]; }
  / 'row-jump' ws '(' ws n:integer ws ')' { return ['row-jump', n]; }
