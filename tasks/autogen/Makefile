.SECONDARY:

clean:
	rm -f *.java
	rm -f *.class
	rm -f domain.txt

domain.java: domain.lisp
	java JSHOP2.InternalDomain $<

%.java: %.lisp domain.class
	java JSHOP2.InternalDomain -r $<

%_run.java:
	echo "public class $*_run { public static void main(String[] args) { System.out.println($*.getPlans()); } }" > $@
	
run_%: domain.class %.class %_run.class
	java $*_run

%.class: %.java
	javac $<

%.plan: domain.class %.class %_run.class
	java $*_run > $@
