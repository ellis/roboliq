package roboliq.processor

object Status extends Enumeration {
	val NotReady, Ready, Success, Error = Value
	//Initialized, Running, LocalSuccess, Error, Complete = Value
}