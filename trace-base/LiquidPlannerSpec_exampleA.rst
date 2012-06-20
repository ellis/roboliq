
Example A
=========


Solvents
--------

:C1: A
:C2: B

Solutes
-------

:C3: C
:C4: D
:C5: E

Destinations
------------

.. math::

  \mathbf{D} = \left[ \begin{array}{} 99 ul & 99 ul \\ 1 ul & 1 ul \\ 0.1 & 0.1 \\ 0.1 & 0.0 \\ 0.0 & 0.1 \end{array} \right]


Sources
-------

.. math::

  \mathbf{S} = \left[ \begin{array}{} 1 ml & 950 ul & 1 ml & 1 ml & 1 ml \\ 0.0 & 50 ul & 0.0 & 0.0 & 0.0 \\ 0.0 & 0.0 & 1 & 0.0 & 0.0 \\ 0.0 & 0.0 & 0.0 & 1 & 0.0 \\ 0.0 & 0.0 & 0.0 & 0.0 & 1 \end{array} \right]

.. math::

  \mathbf{S_{100 ul}} = \left[ \begin{array}{} 1.0 & 0.95 & 1.0 & 1.0 & 1.0 \\ 0.0 & 0.05 & 0.0 & 0.0 & 0.0 \\ 0.0 & 0.0 & 0.01 & 0.0 & 0.0 \\ 0.0 & 0.0 & 0.0 & 0.01 & 0.0 \\ 0.0 & 0.0 & 0.0 & 0.0 & 0.01 \end{array} \right]


Direct solution
---------------

.. math::

  \mathbf{X} = \left[ \begin{array}{} 60.0 & 60.0 \\ 20.0 & 20.0 \\ 10.0 & 10.0 \\ 10.0 & 0.0 \\ 0.0 & 10.0 \end{array} \right]


Step Iteration #0
-----------------


.. graphviz::

  digraph G {
  { rank = same; S0; S1; S2; S3; S4; }
  { rank = same; D0; D1; }
  S3 -> D0 [label="10.0"];
  S2 -> D0 [label="10.0"];
  S1 -> D0 [label="20.0"];
  S0 -> D0 [label="60.0"];
  S4 -> D1 [label="10.0"];
  S2 -> D1 [label="10.0"];
  S1 -> D1 [label="20.0"];
  S0 -> D1 [label="60.0"];
  }


Step Iteration #1
-----------------


.. graphviz::

  digraph G {
  { rank = same; S0; S1; S2; S3; S4; }
  { rank = same; D0; D1; }
  S3 -> D0 [label="10.0"];
  S2 -> D0 [label="10.0"];
  T0 -> D0 [label="80.0"];
  S4 -> D1 [label="10.0"];
  S2 -> D1 [label="10.0"];
  T0 -> D1 [label="80.0"];
  S0 -> T0 [label="120.0"];
  S1 -> T0 [label="40.0"];
  }


Step Iteration #2
-----------------


.. graphviz::

  digraph G {
  { rank = same; S0; S1; S2; S3; S4; }
  { rank = same; D0; D1; }
  S3 -> D0 [label="10.0"];
  T1 -> D0 [label="90.0"];
  S4 -> D1 [label="10.0"];
  T1 -> D1 [label="90.0"];
  S0 -> T0 [label="120.0"];
  S1 -> T0 [label="40.0"];
  S2 -> T1 [label="20.0"];
  T0 -> T1 [label="160.0"];
  }


Step Iteration #3
-----------------


.. graphviz::

  digraph G {
  { rank = same; S0; S1; S2; S3; S4; }
  { rank = same; D0; D1; }
  S3 -> D0 [label="10.0"];
  T1 -> D0 [label="90.0"];
  S4 -> D1 [label="10.0"];
  T1 -> D1 [label="90.0"];
  S0 -> T1 [label="120.0"];
  S1 -> T1 [label="40.0"];
  S2 -> T1 [label="20.0"];
  }
