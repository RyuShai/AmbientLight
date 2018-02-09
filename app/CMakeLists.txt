# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.
cmake_minimum_required(VERSION 3.4.1)

# point to local openCV installation, relative to the directory of CMakeLists.txt
set(pathToOpenCv /home/ryu/Android/OpenCV-android-sdk) #sua lai duong dan cho nay

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(${pathToOpenCv}/sdk/native/jni/include)
add_library(native-lib SHARED
            src/main/cpp/native-lib.cpp )

add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

target_link_libraries(native-lib log lib_opencv)
