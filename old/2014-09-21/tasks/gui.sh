#!/bin/bash

scala -e "
import JSHOP2._

$1.getPlans();
new JSHOP2GUI();
"
