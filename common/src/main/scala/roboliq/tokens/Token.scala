package roboliq.tokens

import roboliq.robot.RobotState

abstract class Token(val name: String)
abstract class T0_Token(name: String) extends Token(name)
abstract class T1_Token(name: String) extends Token(name)
abstract class T2_Token(name: String) extends Token(name)

case class TokenError(val message: String) extends Token("error")
case class T0_TokenError(val message: String) extends T0_Token("error")
case class T1_TokenError(val message: String) extends T1_Token("error")

class T1_TokenState(val tok: T1_Token, val state: RobotState)
