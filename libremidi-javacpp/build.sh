#!/bin/bash

mkdir -p build/$PLATFORM
cd build/$PLATFORM

DIST=`pwd`

set -e

PWD=`pwd`

pushd .
cd ../../../external/libremidi
if [ ! -f .patch.stamp ] ; then
    patch -i ../../javacpp-fix.patch -p1
    touch .patch.stamp
fi
cmake -B build-$PLATFORM -DBUILD_SHARED_LIBS=false -DCMAKE_C_FLAGS=-fPIC -DCMAKE_CXX_FLAGS=-fPIC -DCMAKE_BUILD_TYPE=Release
cmake --build build-$PLATFORM
echo "PWD: $PWD"
echo "DIST: $DIST"
cmake --install build-$PLATFORM --prefix $DIST

popd
