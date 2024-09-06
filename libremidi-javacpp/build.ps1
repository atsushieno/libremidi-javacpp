
$PLATFORM = "$Env:PLATFORM"
New-Item -ItemType Directory -Force -Path "build\$PLATFORM"
Set-Location "build\$PLATFORM"

$DIST = (Get-Location).Path

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $PSCommandPath

echo "DIST: $DIST"
echo "ScriptDir: $ScriptDir"

dir
Set-Location "$ScriptDir\..\external\libremidi"
if (-not (Test-Path .patch.stamp)) {
    patch -i $ScriptDir\..\javacpp-fix.patch -p1
    New-Item -ItemType File -Name .patch.stamp
}
cmake -B "build-$PLATFORM" -DBUILD_SHARED_LIBS=false -G "NMake Makefiles" -DCMAKE_BUILD_TYPE=Release
cmake --build "build-$PLATFORM"

cmake --install "build-$PLATFORM" --prefix $DIST
#New-Item -ItemType Directory -Path "$DIST\include\libremidi" -Force
#New-Item -ItemType Directory -Path "$DIST\lib" -Force
#Copy "*.h" "$DIST\include\libremidi"
#Copy "build-$PLATFORM\Debug\*" "$DIST\lib"

