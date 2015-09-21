/*
 * displayBlocks
 *
 * Warren Sack <wsack@ucsc.edu>
 * November 2010
 *
 * Instructions are assumed to be a list of statements computed by the
 * planner.  They are of three kinds:
 *
 * (1) strings; e.g., "a": these are the labels for the blocks; they also
 *     function as block declarations: each label implies a block;
 * (2) "on" statements; e.g., {"on": {"above": "b", "below": "table"}}: these
 *     declare the initial positions of the blocks; and,
 * (3) "move" statements; e.g., {"move": {"block": "?block", "from": "?from", "to": "?to"}}:
 *     these are to be interpreted as block movements
 *
 * The functions in this file accept a list of instructions, create the necessary HTML,
 * and animate the "move" instructions by moving the HTML DIVs on the screen.
 *
 */
function makeDisplay (utils,instructions) {

  ////
  //// global variables
  ////

  //
  // leftOfDisplay
  //
  var leftOfDisplay = 0;

  //
  // topOfDisplay
  //
  var topOfDisplay = 100;
  
  //
  // borderWidth
  //
  var borderWidth = 5;
  
  //
  // blockWidth
  //
  var blockWidth;

  //
  // tableTop
  //
  var tableTop = window.innerHeight - 100;

  //
  // tableHeight
  //
  var tableHeight = 20;

  //
  // blocks
  //
  var blocks = [];

  //
  // idNumber
  //
  var idNumber = 0;

  //
  // blockDivs
  //
  var blockDivs = {};

  //
  // intervalID: for the animation functions
  //
  var intervalID;

  //
  // currentMove: for the animation functions
  //
  var currentMove;

  //
  // waitBetweenMoves: for the animation functions
  //
  var waitBetweenMoves = 4000;

  //
  // waitBetweenAnimationFrames: for the animation functions
  //
  var waitBetweenAnimationFrames = 1;



  ////
  //// auxilary and accessor functions
  ////

  //
  // intToPXString
  //
  function intToPXString (int) { return(int + "px"); }

  //
  // pxStringToInt
  //
  function pxStringToInt (pxString) { return(pxString.replace(/px/i,"")) / 1; }

  //
  // makeID
  //
  // Make a unique id name.
  //
  function makeID () { idNumber++; return('id_' + idNumber); }

  //
  // getLabels(array): array
  //
  // Get the blocks' labels from the list of instructions.
  //
  function getLabels(instructions) {
    var labels = [];
    for ( var i = 0; i < instructions.length; i++ ) {
      if ( utils.typeOf(instructions[i]) === 'string' ) { labels.push(instructions[i]); }
    }
    return(labels);
  }

  //
  // getOns(array): array
  //
  // Get the {"on": statements from the list of instructions.
  //
  function getOns(instructions) {
    var ons = [];
    for ( var i = 0; i < instructions.length; i++ ) {
      if ( ( utils.typeOf(instructions[i]) === 'object' ) 
	   && ( utils.predicateName(instructions[i]) == "on" ) ) {
	ons.push(instructions[i]); 
      }
    }
    return(ons);
  }

  //
  // getMoves(array): array
  //
  // Get the {"move" statements from the instructions.
  //
  function getMoves(instructions) {
    var moves = [];
    for ( var i = 0; i < instructions.length; i++ ) {
      if ( ( utils.typeOf(instructions[i]) === 'object' ) 
	   && ( utils.predicateName(instructions[i]) == "move" ) ) {
	moves.push(instructions[i]); 
      }
    }
    return(moves);
  }
  

  //
  // translateToAttributeToLeftCoordinate(move): integer
  //
  // Given a {"move": {"block": "?block", "from": "?from", "to": "?to"}}
  // find the left coordinate of the block (or table) in the "to": slot.
  //
  function translateToAttributeToLeftCoordinate(move) {
    var left;
    var blockDiv = blockDivs[move["move"]["block"]];
    // Find the destination coordinate
    if ( move["move"]["to"] == "table" ) { 
      left = blockDiv.tableLeft; }
    
    else {
      var toDiv = blockDivs[move["move"]["to"]];
      left = pxStringToInt(toDiv.style.left); 
    }
    return(left);
  }

  //
  // translateToAttributeToTopCoordinate(move): integer
  //
  // Given a {"move": {"block": "?block", "from": "?from", "to": "?to"}}
  // find the top coordinate of the block (or table) in the "to": slot.
  //
  function translateToAttributeToTopCoordinate(move) {
    var top;
    var blockDiv = blockDivs[move["move"]["block"]];
    // Find the destination coordinate
    if ( move["move"]["to"] == "table" ) { 
      top = tableTop - blockWidth + 1;
    }
    else {
      var toDiv = blockDivs[move["move"]["to"]];
      top = pxStringToInt(toDiv.style.top) - blockWidth + 1; 
    }
    return(top);
  }



  ////
  //// graphics functions
  ////

  // 
  // makeDiv(id,label,left,top,width,height,visibility,outlineColor,backgroundColor):
  //    side effects DOM
  //
  // Make an HTML div with the specified id and positioned 
  // on the screen at left and top.  
  //
  function makeDiv (id,label,left,top,width,height,visibility,outlineColor,backgroundColor) {
    var newDiv = document.createElement("div");
    newDiv.id = id;
    newDiv.style.width = width;
    newDiv.style.height = height;
    newDiv.style.position = "absolute";
    newDiv.style.visibility = visibility;      
    newDiv.style.outlineColor = outlineColor;
    newDiv.style.outlineStyle = "none";
    newDiv.style.outlineWidth = "thin";
    newDiv.style.left = intToPXString(left);
    newDiv.style.top = intToPXString(top);
    newDiv.style.backgroundColor = backgroundColor; 
    newDiv.style.color = "white";
    newDiv.style.fontFamily = "Arial";
    newDiv.style.fontSize = "24px";
    newDiv.innerHTML = "&nbsp;" + label; // + "<BR>" + backgroundColor;
    document.body.appendChild(newDiv);
    return(newDiv);
  }


  //
  // makeBlockDivs(array): side effects DOM
  //
  // Create the divs for all of the blocks.
  //
  function makeBlockDivs(labels) {
    var left = leftOfDisplay + borderWidth;
    var blockDiv;
    for ( var i = 0; i < labels.length; i++ ) {
      var top = tableTop - blockWidth;
      var visibility = "visible"; // "hidden"
      var outlineColor = "black";
      var redComponent = Math.round(0 * Math.random());
      var greenComponent = Math.round(256 * Math.random());
      var blueComponent = Math.round(256 * Math.random());
      var backgroundColor = "rgba(" + redComponent + "," + greenComponent + "," + blueComponent + ",0.5)";
      blockDiv = makeDiv(makeID(),labels[i],left,top,blockWidth,blockWidth,visibility,outlineColor,backgroundColor);
      blockDiv.label = labels[i];
      blockDiv.tableLeft = left;
      blockDivs[labels[i]] = blockDiv;
      left += blockWidth + borderWidth;
    }
    return(blockDivs);
  }

  //
  // interpretOns(array): side effects DOM
  //
  // Given a list of {"on": {"above": "X", "below": "Y"}} statements move (left,top) coordinates
  // of the div representing the block xX so that its Left coordinate matches the left coordinate
  // of block Y and so that its top coordinate is blockWidth above Y's top coordinate.
  //
  function interpretOns(ons) {
    var aboveDiv, belowDiv;
    var movement = true;
    // While the order of the {"on": statements is unknown (and thus a following statement might
    // dictate that the "below" block be moved even after the current statement moves a block
    // on top of it), we assume that the list of {"on": statements describes a stable situation.
    // So, we iterate through the {"on": instructions until none of them have had to be moved.
    // If the list of {"on": statements does not describe a stable situation (e.g., on(a,b) & on(b,a)),
    // this loop will never terminate.
    while( movement ) {
      movement = false;
      for ( var i = 0; i < ons.length; i++ ) {
	aboveDiv = blockDivs[ons[i]["on"]["above"]];
	if ( ons[i]["on"]["below"] != "table" ) {
	  belowDiv = blockDivs[ons[i]["on"]["below"]];
	  if ( ( aboveDiv.style.left != belowDiv.style.left ) 
	       || ( pxStringToInt(aboveDiv.style.top) != ( pxStringToInt(belowDiv.style.top) - blockWidth ) ) ) {
	    movement = true;
	    aboveDiv.style.left = belowDiv.style.left;
	    aboveDiv.style.top = intToPXString(pxStringToInt(belowDiv.style.top) - blockWidth);
	  }
	}
      }
    }
  }
	
  //
  // makeTableDiv(): side effects DOM
  //
  // The "table" is represented as a rectangular div positioned at the bottom of the screen.
  //
  function makeTableDiv() {
      return(makeDiv("table","table",leftOfDisplay,tableTop,window.innerWidth,
		   tableHeight,"visible","black","black"));
  }

  //
  // initializeWindow(array,array): side effects DOM and thus the screen
  //
  // Make the blocks small enough so that all of them can fit on the table at once.
  // Make all of the divs for the blocks and position them according to the {"on": statements.
  //
  function initializeWindow(labels,ons) {
    window.resizeTo(screen.availWidth,screen.availHeight);
    blockWidth = Math.round( ( window.innerWidth - ( labels.length * borderWidth ) 
			       - ( 5 * borderWidth ) ) / labels.length );
    makeTableDiv();
    makeBlockDivs(labels);
    interpretOns(ons);
  }



  ////
  //// animation functions
  ////

  //
  // moveBlockDiv(div,integer,integer): side effects DOM
  //
  // Set the top and left coordinates of the div to the given coordinates.
  //
  function moveBlockDiv(blockDiv,left,top) {
    blockDiv.style.left = intToPXString(left);
    blockDiv.style.top = intToPXString(top);
  }
    
  //
  // animateMove(moveStatement): side effects DOM and thus screen
  //
  // Show the movement of a block's DIV from its current position to it destination.
  //
  function animateMove(move) {
    var deltaH, deltaV, nextLeft, nextTop;
    var increment = 1;
    var blockDiv = blockDivs[move["move"]["block"]];
    // Find the current coordinates of the blockDiv
    var fromLeft = pxStringToInt(blockDiv.style.left);
    var fromTop = pxStringToInt(blockDiv.style.top);
    // Find the destination coordinates of the blockDiv
    var toLeft = translateToAttributeToLeftCoordinate(move);
    var toTop = translateToAttributeToTopCoordinate(move);
    // Determine if the blockDiv should move to the left
    // or right.
    if ( fromLeft < toLeft ) { deltaH = increment; }
    else { deltaH = 0 - increment; }
    var distanceToLeft = Math.abs(fromLeft - toLeft);
    // Calculate the horizontal move.
    if ( distanceToLeft > increment ) { nextLeft = fromLeft + deltaH; }
    // If the blockDiv has almost reached the target horizontal position,
    // assign it the exact target horizontal position.
    else { nextLeft = toLeft; }
    // Determine if the blockDiv should move up or down.
    if ( fromTop < toTop ) { deltaV = increment; }
    else { deltaV = 0 - increment; }
    var distanceToTop = Math.abs(fromTop - toTop);
    // Calculate the vertical move.
    if ( distanceToTop > increment ) { nextTop = fromTop + deltaV; }
    // If the blockDiv has almost reached the target vertical position,
    // assign it the exact target vertical position.
    else { nextTop = toTop; }
    // Move the block's div to its next position.
    moveBlockDiv(blockDiv,nextLeft,nextTop);
    // If blockDiv is not at the correct horizontal and vertical
    // coordinates, then keep it moving (possibily vertically and horizontally).
    if ( ( distanceToLeft > increment ) || ( distanceToTop > increment ) ) {
      var nextStep = function () { animateMove(move); };
      setTimeout(nextStep,waitBetweenAnimationFrames);
    }
  }

  //
  //
  // animateMoves(moves): side effects screen
  //
  // Animate all of the move instructions.
  //
  function animateMoves(moves) {
    if ( currentMove < moves.length ) {
      animateMove(moves[currentMove]);
      currentMove++;
    }
    else { clearInterval(intervalID); }
  }

  //
  // showAnimation(): side effects screen
  //
  // Set the size of the window, determine the size of the blocks,
  // create an HTML div element for each block, initialize the
  // blocks into their starting positions, move the blocks
  // according to the list of {"move" statements.
  //
  function showAnimation() {
    initializeWindow(getLabels(instructions),getOns(instructions));
    currentMove = 0;
    var moves = getMoves(instructions);
    var nextStep = function () { animateMoves(moves); };
    intervalID = setInterval(nextStep,waitBetweenMoves); 
  }
    

  // Return the methods for displaying the animated blocks
  return({showAnimation : showAnimation});


} // end of makeDisplay













