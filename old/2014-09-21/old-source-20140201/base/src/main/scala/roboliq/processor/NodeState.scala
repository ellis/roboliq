package roboliq.processor

import roboliq.core._


class NodeState(val node: Node) {
	private var _status = Status.NotReady 
	private var _input_l = List[Object]()
	private var _inputResult: RqResult[Unit] = RqResult.zero
	private var _functionResult: RqResult[Unit] = RqResult.zero
	private var _child_l = List[Node]()
	
	def status = _status
	def inputResult = _inputResult
	def functionResult = _functionResult
	def result = _inputResult.flatMap(_ => _functionResult)
	def child_l = _child_l
	
	def updateInput(getEntity: KeyClassOpt => RqResult[Object]) {
		if (node.input_l.isEmpty && _status == Status.NotReady) {
			_status = Status.Ready
		}
		else {
			val result = RqResult.toResultOfList(node.input_l.map(getEntity))
			result match {
				case RqSuccess(input2_l, _) =>
					// If inputs have changed, then the function should be recomputed.
					if (_input_l != input2_l) {
						_status = Status.Ready
						_input_l = input2_l
						_inputResult = result.map(_ => ())
						_functionResult = RqResult.zero
						_child_l = Nil
					}
				// Error
				case _ =>
					_input_l = Nil
					_inputResult = result.map(_ => ())
					_functionResult = RqResult.zero
					_child_l = Nil
			}
		}
	}
	
	private def reset() {
		_functionResult = RqResult.zero
		_child_l = Nil
	}

	def setFunctionResult(result: scala.util.Try[RqResult[_]]) {
		val (s, r: RqResult[_]) = result match {
			case scala.util.Success(r) =>
				r match {
					case RqSuccess(l, _) => (Status.Success, r)
					case RqError(_, _) => (Status.Error, r)
				}
			case scala.util.Failure(e) =>
				(Status.Error, RqError[Unit](e.getMessage()))
		}
		_status = s
		_functionResult = r.map(_ => ())
		//println("node: "+node+", status: "+s)
	}
	
	def setChildren(child_l: List[Node]) {
		_child_l = child_l
	}
}
