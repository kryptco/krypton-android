#include <jni.h>
#include <string>

extern "C"
jstring
Java_co_krypt_kryptonite_MeActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
