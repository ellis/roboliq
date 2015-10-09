From within sbt:

    project evoware
    run ../testdata/bsse-mario/Carrier.cfg
    run ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt
    run ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt ../roboliq/protocols/output/protocol2.out.json

The first `run` above will display carrier information.
The second `run` above will display table information.
The last `run` above will compile the protocol and output a file named `test.esc`.


# Running roboliq as an assembly

Create the assembly:

    cd source
    sbt
    project evoware
    assembly

Run an example:

    java -jar ./evoware/target/scala-2.11/base-assembly-0.1.jar ../testdata/bsse-mario/Carrier.cfg ../testdata/bsse-mario/NewLayout_Feb2015.ewt ../roboliq/protocols/output/protocol2.out.json

