From the command line:
   ln -s ../tasks tasks
   ln -s ../testdata testdata

From within sbt:

    project base
    run-main roboliq.main.Main --config tasks/autogen/roboliq.yaml --output temp --protocol tasks/autogen/test_single_pipette_01.prot
