Planner Notes
=============

Planner experiences
-------------------

pddl4j: nice PDDL3 parser
pddl4j/Graphplan: ignores negative preconditions on actions
tempo-sat-sgplan6: written in C; seems to work; a small inconvenience is that there needs to be at least one object instance of every type
javaff: doesn't handle negative preconditions
Metric-FF: doesn't handling typing
seq-sat-lama: didn't like my pddl problem file

To run sgplan6:

.. code-block:: sh

  cd /home/public/src/extern/planning/tempo-sat-sgplan6
  ./sgplan -o /home/public/src/roboliq/planner/lhr05.pddl -f /home/public/src/roboliq/planner/lhr05-p1.pddl
