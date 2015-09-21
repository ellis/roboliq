#export CLASSPATH="/home/ellisw/src/extern/TL-SHOP/bin/antlr.jar:/home/ellisw/src/extern/TL-SHOP/bin/JSHOP2.jar:."

CLASSPATH=.
prepend_path()
{
  if ! eval test -z "\"\${$1##*:$2:*}\"" -o -z "\"\${$1%%*:$2}\"" -o -z "\"\${$1##$2:*}\"" -o -z "\"\${$1##$2}\"" ; then
    eval "$1=$2:\$$1"
  fi
}

JSHOP2PATH=~/src/extern/JSHOP2GUI_1.0.1
prepend_path CLASSPATH $JSHOP2PATH/antlr.jar
prepend_path CLASSPATH $JSHOP2PATH/bin.build/JSHOP2.jar

echo $CLASSPATH
export CLASSPATH

