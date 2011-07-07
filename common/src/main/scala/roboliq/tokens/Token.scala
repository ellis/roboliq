package roboliq.tokens

import roboliq.robot.RobotState

abstract class Token(val name: String)
abstract class T0_Token(name: String) extends Token(name)
abstract class T1_Token(name: String) extends Token(name)
abstract class T2_Token(name: String) extends Token(name)

class T1_TokenError(message: String) extends T1_Token("error")

class T1_TokenState(tok: T1_Token, state: RobotState)
