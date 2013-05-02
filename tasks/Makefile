clean:
	rm -f lhr*.java
	rm -f *.class

lhr01.class: lhr01.lisp
	java JSHOP2.InternalDomain $<

lhr01_p2: lhr01_p2.lisp lhr01.class
	java JSHOP2.InternalDomain -r $<
	javac $@.java
	scala -e "println($@.getPlans())"

lhr01_p3: lhr01_p3.lisp lhr01.class
	java JSHOP2.InternalDomain -r $<
	javac $@.java
	scala -e "println($@.getPlans())"

lhr01_p4: lhr01_p4.lisp lhr01.class
	java JSHOP2.InternalDomain -r $<
	javac $@.java
	#scala -e "println($@.getPlans())"
	javac gui.java
	java gui

lhr01_p5: lhr01_p5.lisp lhr01.class
	java JSHOP2.InternalDomain -r $<
	javac $@.java
	scala -e "println($@.getPlans())"

lhr02.class: lhr02.lisp
	java JSHOP2.InternalDomain $<

lhr02_p1: lhr02_p1.lisp lhr02.class
	java JSHOP2.InternalDomain -r $<
	javac $@.java
	scala -e "println($@.getPlans())"

lhr02_p2: lhr02_p2.lisp lhr02.class
	java JSHOP2.InternalDomain -r $<
	javac $@.java
	scala -e "println($@.getPlans())"

lhr02_p3: lhr02_p3.lisp lhr02.class
	java JSHOP2.InternalDomain -r $<
	javac $@.java
	scala -e "println($@.getPlans())"

lhr02%: lhr02%.lisp lhr02.class

lhr%: lhr%.lisp
	java JSHOP2.InternalDomain -r $<
	javac $@.java
	echo "public class $@_run { public static void main(String[] args) { System.out.println($@.getPlans()); } }" > $@_run.java
	javac $@_run.java
	java $@_run
