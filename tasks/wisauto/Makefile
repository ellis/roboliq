.SECONDARY:

clean:
	rm -f *.java
	rm -f *.class
	rm -f domain.txt

domain.java: domain.lisp
	java JSHOP2.InternalDomain $<

p%.java: p%.lisp domain.class
	java JSHOP2.InternalDomain -r $<

%.java: %.lisp domain.class
	java JSHOP2.InternalDomain -r $<

%_run.java:
	echo "public class $*_run { public static void main(String[] args) { System.out.println($*.getPlans()); } }" > $@

%.plan: domain.class %.class %_run.class
	java $*_run > $@
	
run_%: domain.class %.class %_run.class
	java $*_run

%.class: %.java
	javac $<
