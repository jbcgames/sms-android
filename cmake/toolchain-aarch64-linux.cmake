# Cross-compilation toolchain: aarch64-linux-gnu (RK3326 / Mali-G31 / GLES 3.0)
set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_PROCESSOR aarch64)

set(CMAKE_C_COMPILER   aarch64-linux-gnu-gcc)
set(CMAKE_CXX_COMPILER aarch64-linux-gnu-g++)

# Target sysroot / library search paths
set(CMAKE_FIND_ROOT_PATH /usr/aarch64-linux-gnu /usr/lib/aarch64-linux-gnu /usr)
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE BOTH)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE BOTH)

# Package config paths for cross compilation
set(ENV{PKG_CONFIG_PATH} "/usr/lib/aarch64-linux-gnu/pkgconfig")
set(ENV{PKG_CONFIG_LIBDIR} "/usr/lib/aarch64-linux-gnu/pkgconfig")

# Force 64-bit architecture without -m64
set(SMS_ARCH 64 CACHE STRING "Target word size" FORCE)
set(SMS_GLES ON CACHE BOOL "Enable OpenGL ES 3.0 backend" FORCE)

# Architecture compilation flags (ARMv8 Cortex-A35 / NEON)
set(CMAKE_C_FLAGS_INIT   "-march=armv8-a+simd -ffp-contract=off")
set(CMAKE_CXX_FLAGS_INIT "-march=armv8-a+simd -ffp-contract=off")
set(CMAKE_EXE_LINKER_FLAGS_INIT    "-static-libstdc++ -static-libgcc")
set(CMAKE_SHARED_LINKER_FLAGS_INIT "-static-libstdc++ -static-libgcc")

# Portable RPATH for PortMaster
set(CMAKE_BUILD_RPATH   "$ORIGIN:$ORIGIN/libs.aarch64")
set(CMAKE_INSTALL_RPATH "$ORIGIN:$ORIGIN/libs.aarch64")
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
