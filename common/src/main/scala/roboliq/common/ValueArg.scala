package roboliq.common

/*
sealed class LocationArg private (arg: Either[String, LocationConfig]) {
	def location(states: StateMap) = arg match {
		case Left(s) => s
		case Right(location) => location.state(states).location
	}
}

object LocationArg {
	def apply(location: String) = new LocationArg(Left(location))
	def apply(location: LocationConfig) = new LocationArg(Right(location))
}
*/
sealed class ValueArg[T] private (arg: Either[T, MementoConfig[T]]) {
	def value(states: StateMap) = arg match {
		case Left(v) => v
		case Right(m) => m.state(states).value
	}
}

object ValueArg {
	def apply[T](v: T) = new ValueArg[T](Left(v))
	def apply[T](m: MementoConfig[T]) = new ValueArg[T](Right(m))
}
