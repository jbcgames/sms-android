cmake_minimum_required(VERSION 3.22)
project(sms_android C CXX)

set(SMS_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../..")
get_filename_component(SMS_ROOT "${SMS_ROOT}" ABSOLUTE)

# Build SDL2 from bundled source
set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
add_subdirectory("${SMS_ROOT}/third_party/SDL2-android" SDL2)

set(SMS_GLES ON CACHE BOOL "" FORCE)
set(SMS_ARCH "64" CACHE STRING "" FORCE)

# Point to real python executable for code generator scripts
if(EXISTS "C:/Users/migue/AppData/Local/Programs/Python/Python313/python.exe")
  set(Python3_EXECUTABLE "C:/Users/migue/AppData/Local/Programs/Python/Python313/python.exe" CACHE FILEPATH "" FORCE)
endif()

add_subdirectory("${SMS_ROOT}" sms_core)
