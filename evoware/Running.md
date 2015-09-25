From within sbt:

    project base2
    run-main roboliq.evoware.translator.EvowareCompilerMain ../testdata/bsse-mario/Carrier.cfg
    run-main roboliq.evoware.translator.EvowareCompilerMain ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt
    run-main roboliq.evoware.translator.EvowareCompilerMain ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt ../roboliq/protocols/output/protocol2.out.json

The first `run-main` above will display carrier information.
The second `run-main` above will display table information.
The last `run-main` above will compile the protocol and output a file named `test.esc`.


# Running roboliq as an assembly

Create the assembly:

    cd source
    sbt
    project base
    assembly

Run an example:

    java -jar /home/ellisw/src/roboliq/source/base/target/scala-2.10/base-assembly-0.1.jar --config tasks/autogen/roboliq.yaml --protocol tasks/autogen/alan01.prot
