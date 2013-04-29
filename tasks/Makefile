
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
