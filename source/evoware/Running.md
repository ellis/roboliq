From the command line:
   ln -s ../tasks tasks
   ln -s ../testdata testdata

From within sbt:

    project base
    run-main roboliq.main.Main --config tasks/autogen/roboliq.yaml --output temp --protocol tasks/autogen/test_single_pipette_01.prot

# Running roboliq as an assembly

Create the assembly:

    cd source
    sbt
    project base
    assembly

Run an example:

    java -jar /home/ellisw/src/roboliq/source/base/target/scala-2.10/base-assembly-0.1.jar --config tasks/autogen/roboliq.yaml --protocol tasks/autogen/alan01.prot
