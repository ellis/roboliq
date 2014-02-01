# Running roboliq

Create the assembly:

    cd source
    sbt
    project base
    assembly

Run an example:

    java -jar /home/ellisw/src/roboliq/source/base/target/scala-2.10/base-assembly-0.1.jar --config tasks/autogen/roboliq.yaml --protocol tasks/autogen/alan01.prot
