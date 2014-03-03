package aiplan.strips2

import scala.util.parsing.combinator.JavaTokenParsers
import scalaz._
import Scalaz._


sealed trait LispElem
case class LispString(s: String) extends LispElem
case class LispList(l: List[LispElem]) extends LispElem


object PddlParser {
	val test = """
(define (domain random-domain-fe)
  (:requirements :strips)
  (:action op1
    :parameters (?x1 ?x2 ?x3)
    :precondition (and (R ?x3 ?x1) (S ?x2 ?x2))
    :effect (and (R ?x1 ?x3) (S ?x3 ?x1) (not (R ?x3 ?x1)) (not (S ?x2 ?x2))))
  (:action op2
    :parameters (?x1 ?x2 ?x3)
    :precondition (and (R ?x1 ?x2) (S ?x2 ?x1) (R ?x2 ?x3))
    :effect (and (R ?x2 ?x2) (S ?x1 ?x2) (not (S ?x2 ?x1)))))
"""

	type L = LispList
	type S = LispString
	
	def parseDomain(input: String): Either[String, Strips.Domain] = {
		LispParser0.parse(input) match {
			case Left(msg) => Left(msg)
			case Right(elem) =>
				//println(elem)
				elemToDomain(elem)
		}
	}
	
	def parseProblem(domain: Strips.Domain, input: String): Either[String, Strips.Problem] = {
		LispParser0.parse(input) match {
			case Left(msg) => Left(msg)
			case Right(elem) =>
				//println(elem)
				elemToProblem(domain, elem)
		}
	}
	
	def elemToDomain(elem: LispElem): Either[String, Strips.Domain] = {
		elem match {
			case LispList(LispString("define") :: LispList(List(LispString("domain"), LispString(domainName))) :: rest) =>
				val actionDef_l = rest.collect({ case LispList(LispString(":action") :: LispString(actionName) :: rest) => (actionName, rest) })
				for {
					operator_l <- actionDef_l.map(pair => {
						val (name, l) = pair
						val op0 = Strips.Operator(name, Nil, Nil, Strips.Literals.empty, Strips.Literals.empty)
						updateOperator(l, op0)
					}).sequenceU.right
				} yield {
					Strips.Domain(
						type_l = Set(),
						constantToType_m = Map(),
						predicate_l = Nil,
						operator_l = operator_l
					)
				}
			case _ => Left("Unrecognized token in domain: "+elem)
		}
	}
	
	def elemToProblem(domain: Strips.Domain, elem: LispElem): Either[String, Strips.Problem] = {
		elem match {
			case LispList(LispString("define") :: LispList(List(LispString("problem"), LispString(problemName))) :: LispList(List(LispString(":domain"), LispString(domainName))) :: rest) =>
				val initDef_l = rest.collect({ case LispList(LispString(":init") :: rest) => rest }).flatten
				val goalDef_l = rest.collect({ case LispList(List(LispString(":goal"), rest)) => rest })
				for {
					init_l <- initDef_l.map(getAtom).sequenceU.right
					goal_l <- goalDef_l.map(elem => getLiterals(elem)).sequenceU.right
				} yield {
					Strips.Problem(
						domain = domain,
						object_l = Nil,
						state0 = Strips.State(init_l.toSet),
						goals = goal_l.head
					)
				}
			case _ => Left("Unrecognized token in domain: "+elem)
		}
	}
	
	private def updateOperator(l: List[LispElem], acc: Strips.Operator): Either[String, Strips.Operator] = {
		l match {
			case Nil => Right(acc)
			case LispString(":parameters") :: LispList(l2) :: rest =>
				val paramName_l = l2.map(_.asInstanceOf[LispString]).map(_.s)
				val acc2 = Strips.Operator(
					name = acc.name,
					paramName_l = paramName_l,
					paramTyp_l = paramName_l.map(x => "any"),
					preconds = acc.preconds,
					effects = acc.effects
				)
				updateOperator(rest, acc2)
			case LispString(":precondition") :: elem :: rest =>
				val preconds_? = getLiterals(elem).right;
				for {
					preconds <- preconds_?
					acc3 <- updateOperator(rest, Strips.Operator(acc.name, acc.paramName_l, acc.paramTyp_l, preconds, acc.effects)).right
				} yield acc3
			case LispString(":effect") :: elem :: rest =>
				for {
					effects <- getLiterals(elem).right
					acc3 <- updateOperator(rest, Strips.Operator(acc.name, acc.paramName_l, acc.paramTyp_l, acc.preconds, effects)).right
				} yield acc3
			case _ => Left("Unrecognized tokens in operator: "+l)
		}
	}
	
	private type Literal = (Strips.Atom, Boolean)
	
	private def getLiterals(elem: LispElem): Either[String, Strips.Literals] = elem match {
		case LispList(Nil) => Right(Strips.Literals.empty)
		case LispList(LispString("and") :: l) =>
			for {
				literal_l <- l.map(elem => getLiteral(elem, true)).sequenceU.right
			} yield {
				val (pos_l, neg_l) = literal_l.partition(_._2)
				Strips.Literals(pos_l.map(_._1).toSet, neg_l.map(_._1).toSet)
			}
		case _ => Left("unrecognized literals: "+elem)
	}
	
	private def getLiteral(elem: LispElem, pos: Boolean): Either[String, Literal] = {
		elem match {
			case LispList(Nil) => Left("Empty literal not allowed")
			case LispList(LispString("not") :: elem2 :: Nil) =>
				getLiteral(elem2, !pos)
			case LispList(LispString(name) :: l) if l.forall(_.isInstanceOf[LispString]) =>
				val atom = Strips.Atom(name, l.map(_.asInstanceOf[LispString].s))
				Right((atom, pos))
			case _ => Left("unrecognized literal: "+elem)
		}
	}
	
	private def getAtom(elem: LispElem): Either[String, Strips.Atom] = {
		elem match {
			case LispList(Nil) => Left("Empty atom not allowed")
			case LispList(LispString(name) :: l) if l.forall(_.isInstanceOf[LispString]) =>
				Right(Strips.Atom(name, l.map(_.asInstanceOf[LispString].s)))
			case _ => Left("unrecognized atom: "+elem)
		}
	}
	
	//private implicit def listToLisp(l: List[LispElem]): LispElem = LispList(l)
	//private implicit def stringToLisp(s: String): LispElem = LispString(s)
}

private object LispParser0 extends JavaTokenParsers {
	def list: Parser[LispElem] = "(" ~ rep(elem) ~ ")" ^^ {
		case _ ~ l ~ _ => LispList(l)
	}

	def toLispString(p: Parser[String]): Parser[LispElem] = p ^^ { s => LispString(s) }
	
	def lident: Parser[LispElem] = toLispString("""[a-zA-Z?][a-zA-Z0-9_-]*""".r)
	def keyword: Parser[LispElem] = toLispString(""":[a-zA-Z][a-zA-Z0-9_-]*""".r)
	def string: Parser[LispElem] = toLispString(stringLiteral)
	
	def elem: Parser[LispElem] = string | keyword | lident | list 

	def parse(input: String): Either[String, LispElem] = {
		parseAll(elem, input) match {
			case Success(x, _) => Right(x)
			case NoSuccess(msg, _) => Left(msg)
		}
	}
}

/*
private object LispParser1 extends JavaTokenParsers {
	import Strips._
	
	def parens[A](a: Parser[A]): Parser[A] = "(" ~ a ~ ")" ^^ {
		case _ ~ x ~ _ => x
	}
	
	def domain: Parser[Domain] =
		"(define (domain" ~ ident ~ ")" ~ rep(domainProperty) ~ ")" ^^ {
		case _ ~ domainName ~ _ ~ property_l ~_ => LispList(l)
	}
	
	def domainProperty(domain: Domain): Parser[Domain] =
		domainRequirements ^^ {
			case _ => domain
		}
	
	def domainRequirements: Parser[Unit] = parens(":requirements" ~ rep(keyword)) ^^ { x => () }

	def toLispString(p: Parser[String]): Parser[LispElem] = p ^^ { s => LispString(s) }
	
	def ident: Parser[LispElem] = toLispString("""[a-zA-Z?][a-zA-Z0-9_-]*""".r)
	def keyword: Parser[LispElem] = toLispString(""":[a-zA-Z][a-zA-Z0-9_-]*""".r)
	def string: Parser[LispElem] = toLispString(stringLiteral)
	
	def elem: Parser[LispElem] = string | keyword | ident | list 

	def parse(input: String): Either[String, LispElem] = {
		parseAll(elem, input) match {
			case Success(x, _) => Right(x)
			case NoSuccess(msg, _) => Left(msg)
		}
	}
}
*/
/*
private object PddlParser0 extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	//def realConst: Parser[BigDecimal] = """(?:-)?\d+(?:(?:\.\d+E(?:-)?\d+)|(?:\.\d+)|(?:E(?:-)?\d+))""".r ^^ { num =>
	def realConst: Parser[BigDecimal] = """[+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+-]?\d+)?""".r ^^ { num =>
		BigDecimal(num)
	}
	
	def unit: Parser[String] = """[num]?l""".r ^^ { s => s }
	
	def ident: Parser[String] = """[a-zA-Z][a-zA-Z0-9_-]*""".r ^^ { s => s }
	
	def vari: Parser[String] = """[?][a-zA-Z][a-zA-Z0-9_-]*""".r ^^ { s => s }
	
	def domain: Parser[Strips.Domain] =
		"(define (domain" ~ ident ~ opt(requirements) ~ rep1(action) ^^ {
		case _ ~ name ~ _ ~ action_l =>
		
	}
	
	def parens[A](a: Parser[A]): Parser[A] = "(" ~ a ~ ")" ^^ {
		case _ ~ x ~ _ => x
	}
	
	def requirements: Parser[Unit] = "(:requirements" ~ """[^)]*)""".r ^^ { _ => () }
	
	def action: Parser[Strips.Operator] = parens({
		":action" ~ ident ~ ":parameters" ~ parens(rep(vari)) ^^
	})
	
	def complete: Parser[LiquidVolume] = realConst ~ unit ^^ {
		case n ~ u => u match {
			case "nl" => LiquidVolume.nl(n)
			case "ul" => LiquidVolume.ul(n)
			case "ml" => LiquidVolume.ml(n)
			case "l" => LiquidVolume.l(n)
		}
	}
	
	def parseDomain(input: String): Either[String, Strips.Domain] = {
		parseAll(domain, input) match {
			case Success(x, _) => Right(x)
			case NoSuccess(msg, _) => Left(msg)
		}
	}
}
*/