/*
 * logoGraphics
 *
 * Definition of Logo commands in JavaScript.  Use makeLogo()
 * to create an instance of Logo with the forward, back, left, right,
 * penUp, penDown, and penColor methods.
 * Requires an HTML 5 canvas element with id = logoCanvas;
 * i.e., include the following in the BODY of your HTML:
 * <canvas id="logoCanvas" width="500" height="500"></canvas>
 *
 * Warren Sack <wsack@ucsc.edu>
 * December 2009
 *
 */

function makeLogo () {

  // Create a piece of the page to draw on.
  var logoCanvas;
  var logoCanvasContext;

  // 360 degrees equal 2 * PI radians
  var twoPI = 2 * Math.PI;

  // radiansPerDegree
  var radiansPerDegree = twoPI / 360;

  // X position of the "Turtle"
  var currentTurtleXPosition = 200;

  // Y position of the "Turtle"
  var currentTurtleYPosition = 200;

  // Last X position of the "Turtle"
  var previousTurtleXPosition = 200;

  // Last Y position of the "Turtle"
  var previousTurtleYPosition = 200;

  // Angle of the turn underway by the "Turtle"
  var angle = 0;

  // Angle of the last turn accomplished by the "Turtle"
  var lastAngle = 0;

  // isPenUp is true if the "Turtle" is to move, but not leave a trace.
  // isPenUp is false otherwise.
  var isPenUp = false;

  //// The LOGO commands are built on top of the following base commands

  function initializeCanvas() {
    logoCanvas = document.getElementById('logoCanvas'); 
    logoCanvasContext = logoCanvas.getContext('2d');  
  }

  function moveThePen(deltaH,deltaV) {
    // No line is drawn if the pen is up; i.e., if isPenUp == true
    if (! isPenUp) {
      logoCanvasContext.beginPath();
      logoCanvasContext.moveTo(currentTurtleXPosition, 
			       currentTurtleYPosition);
      logoCanvasContext.lineTo((currentTurtleXPosition + deltaH), 
			       (currentTurtleYPosition + deltaV));
      logoCanvasContext.stroke();
    }
  }

  function updateCurrentTurtlePositionAndZeroAngle(deltaH, deltaV) {
    previousTurtleXPosition = currentTurtleXPosition;
    currentTurtleXPosition = currentTurtleXPosition + deltaH;
    previousTurtleYPosition = currentTurtleYPosition;
    currentTurtleYPosition = currentTurtleYPosition + deltaV;
    lastAngle = lastAngle + angle;
    angle = 0;
  }

  // Judge whether the current position of the "Turtle"
  // is closer to or farther from a given point than 
  // the previous position of the "Turtle"
  function isCloserToPoint(x,y) {
    var hp,vp,dp,hc,vc,dc;
    hp = x - previousTurtleXPosition;
    vp = y - previousTurtleYPosition;
    dp = Math.sqrt((hp*hp)+(vp*vp));
    hc = x - currentTurtleXPosition;
    vc = y - currentTurtleYPosition;
    dc = Math.sqrt((hc*hc)+(vc*vc));
    if ( dc < dp ) { return(true); }
    else { return(false); }
  }

  // Determine the distance to a given point.
  function distanceToPoint(x,y) {
    var hc,vc;
    hc = x - currentTurtleXPosition;
    vc = y - currentTurtleYPosition;
    return(Math.sqrt((hc*hc)+(vc*vc)));
  }

  function moveTheTurtle(d) {
    deltaH = (d * Math.cos(lastAngle + angle)) / twoPI;
    deltaV = (d * Math.sin(lastAngle + angle)) / twoPI;
    moveThePen(deltaH, deltaV);
    updateCurrentTurtlePositionAndZeroAngle(deltaH,deltaV);
  }

  //// LOGO COMMANDS: left, right, forward, back, penUp, penDown

  function left(n) {
    n = n * radiansPerDegree;
    angle = angle - n;
  }
  
  function right(n) {
    n = n * radiansPerDegree;
    angle = angle + n;
  }

  function forward(n) {
    moveTheTurtle(n);
  }

  function back(n) {
    moveTheTurtle(-n);
  }

  function penUp() {
    isPenUp = true;
  }

  function penDown() {
    isPenUp = false;
  }

  // Note: colors can be specified using common HTML color
  // names; e.g., "red", "yellow", "green", etc.; or, colors
  // can be specified in hexidecimal form; e.g., "#334455";
  // or, with rgb or rgba (i.e., RGB with an alpha/opacity
  // between 0.0 and 1.0; e.g., "rgba(255,0,0,0.5)"
  function penColor(color) {
    logoCanvasContext.strokeStyle = color;
  }

  function penWidth(width) {
    logoCanvasContext.lineWidth = width;
  }

  return {
      initializeCanvas : initializeCanvas,
      left : left,
      right : right,
      forward : forward,
      back : back,
      penUp : penUp,
      penDown : penDown,
      penColor : penColor,
      penWidth : penWidth,
      distanceToPoint : distanceToPoint,
      isCloserToPoint : isCloserToPoint
  };

}; // end of makeLogo()


