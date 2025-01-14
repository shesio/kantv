/*
 * Copyright (c) 2024- KanTV Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The above statement and notice must be included in corresponding files
 * in derived project
 */
#include <jni.h>

#include "whispercpp/whisper.h"

#include "llamacpp/llama.h"

#include "kantv-asr.h"
#include "ggml-jni.h"

#define UNUSED(x)       (void)(x)

JNIEXPORT jstring JNICALL
Java_org_ggml_ggmljava_asr_1get_1systeminfo(JNIEnv *env, jclass clazz) {
    UNUSED(env);

    LOGGD("enter getSystemInfo");
    const char *sysinfo = whisper_print_system_info();
    jstring string = (*env)->NewStringUTF(env, sysinfo);
    LOGGD("leave getSystemInfo");

    return string;
}


JNIEXPORT void JNICALL
Java_org_ggml_ggmljava_asr_1set_1benchmark_1status(JNIEnv *env, jclass clazz,
                                                           jint b_exit_benchmark) {
    UNUSED(env);
    UNUSED(clazz);

    whisper_set_benchmark_status((int) b_exit_benchmark);
}


JNIEXPORT jstring JNICALL
Java_org_ggml_ggmljava_ggml_1bench(JNIEnv *env, jclass clazz, jstring model_path,
                                       jstring user_data, jint bench_type, jint num_threads, jint backend_type, jint op_type) {
    UNUSED(clazz);

    const char *sz_model_path = NULL;
    const char *sz_user_data = NULL;
    const char *sz_bench_result = "unknown";
    const char *bench_result = NULL;

    sz_model_path = (*env)->GetStringUTFChars(env, model_path, NULL);
    if (NULL == sz_model_path) {
        LOGGW("JNI failure, pls check why?");
        goto failure;
    }

    sz_user_data = (*env)->GetStringUTFChars(env, user_data, NULL);
    if (NULL == sz_user_data) {
        LOGGW("JNI failure, pls check why?");
        goto failure;
    }

    LOGGV("model path:%s\n", sz_model_path);
    LOGGV("user_data:%s\n", sz_user_data);
    LOGGV("bench type: %d\n", bench_type);
    LOGGV("thread counts:%d\n", num_threads);
    LOGGV("backend type:%d\n", backend_type);
    LOGGV("op type:%d\n", op_type);

    if (bench_type > BENCHMAKR_MAX) {
        LOGGW("pls check bench type\n");
        GGML_JNI_NOTIFY("benchmark type %d not supported currently", bench_type);
        goto failure;
    }

    if (backend_type > BACKEND_MAX) {
        LOGGW("pls check backend type\n");
        goto failure;
    }

    if (0 == num_threads)
        num_threads = 1;

    ggml_jni_bench(sz_model_path, sz_user_data, bench_type, num_threads, backend_type, op_type);

    if (BECHMARK_ASR == bench_type) { // asr
        //just return "asr_result" even get correct asr result because I'll try to do everything in native layer
        sz_bench_result = "asr_result";
    }

failure:
    if (NULL != sz_model_path) {
        (*env)->ReleaseStringUTFChars(env, model_path, sz_model_path);
    }

    if (NULL != sz_user_data) {
        (*env)->ReleaseStringUTFChars(env, user_data, sz_user_data);
    }

    jstring string = (*env)->NewStringUTF(env, sz_bench_result);
    return string;
}


JNIEXPORT jint JNICALL
Java_org_ggml_ggmljava_get_1cpu_1core_1counts(JNIEnv *env, jclass clazz) {
    UNUSED(env);
    UNUSED(clazz);

    return whisper_get_cpu_core_counts();
}


JNIEXPORT jint JNICALL
Java_org_ggml_ggmljava_asr_1init(JNIEnv *env, jclass clazz, jstring model_path, jint n_threads, jint n_asrmode, jint n_backend) {
    UNUSED(clazz);

    int result  = 0;
    const char *sz_model_path = NULL;

    sz_model_path = (*env)->GetStringUTFChars(env, model_path, NULL);
    if (NULL == sz_model_path) {
        result = 1;
        LOGGW("JNI failure, pls check why?");
        goto failure;
    }

    LOGGV("model path:%s\n", sz_model_path);
    LOGGV("thread counts:%d\n", n_threads);
    LOGGV("asr mode:%d\n", n_asrmode);
    LOGGV("backend type: %d\n", n_backend);

    result = whisper_asr_init(sz_model_path, n_threads, n_asrmode, n_backend);

failure:
    if (NULL != sz_model_path) {
        (*env)->ReleaseStringUTFChars(env, model_path, sz_model_path);
    }

    return result;
}


JNIEXPORT void JNICALL
Java_org_ggml_ggmljava_asr_1finalize(JNIEnv *env, jclass clazz) {
    UNUSED(env);
    UNUSED(clazz);

    whisper_asr_finalize();
}

JNIEXPORT jint JNICALL
Java_org_ggml_ggmljava_asr_1reset(JNIEnv *env, jclass clazz, jstring str_model_path,
                                               jint n_thread_counts, jint n_asrmode, jint n_backend) {
    UNUSED(clazz);

    int result  = 0;
    const char *sz_model_path = NULL;

    sz_model_path = (*env)->GetStringUTFChars(env, str_model_path, NULL);
    if (NULL == sz_model_path) {
        result = 1;
        LOGGW("JNI failure, pls check why?");
        goto failure;
    }

    LOGGV("model path:%s\n", sz_model_path);
    LOGGV("thread counts:%d\n", n_thread_counts);
    LOGGV("asr mode:%d\n", n_asrmode);
    LOGGV("backend type: %d\n", n_backend);

    result = whisper_asr_reset(sz_model_path, n_thread_counts, n_asrmode, n_backend);

failure:
    if (NULL != sz_model_path) {
        (*env)->ReleaseStringUTFChars(env, str_model_path, sz_model_path);
    }

    return result;
}


JNIEXPORT void JNICALL
Java_org_ggml_ggmljava_asr_1start(JNIEnv *env, jclass clazz) {
    UNUSED(env);
    UNUSED(clazz);

    whisper_asr_start();
}

JNIEXPORT void JNICALL
Java_org_ggml_ggmljava_asr_1stop(JNIEnv *env, jclass clazz) {
    UNUSED(env);
    UNUSED(clazz);

    whisper_asr_stop();
}


JNIEXPORT jstring JNICALL
Java_org_ggml_ggmljava_llm_1get_1systeminfo(JNIEnv *env, jclass clazz) {
    UNUSED(env);

    LOGGD("enter getSystemInfo");
    const char *sysinfo = llama_print_system_info();
    jstring string = (*env)->NewStringUTF(env, sysinfo);
    LOGGD("leave getSystemInfo");

    return string;
}


JNIEXPORT jstring  JNICALL
Java_org_ggml_ggmljava_llm_1inference(JNIEnv *env, jclass clazz, jstring model_path, jstring prompt,
                                               jint n_bench_type, jint n_thread_counts, jint n_backend) {
    UNUSED(clazz);

    const char *sz_model_path = NULL;
    const char *sz_prompt = NULL;
    const char *sz_bench_result = "unknown";
    const char *bench_result = NULL;
    int result = 0;

    sz_model_path = (*env)->GetStringUTFChars(env, model_path, NULL);
    if (NULL == sz_model_path) {
        LOGGW("JNI failure, pls check why?");
        goto failure;
    }

    sz_prompt = (*env)->GetStringUTFChars(env, prompt, NULL);
    if (NULL == sz_prompt) {
        LOGGW("JNI failure, pls check why?");
        goto failure;
    }

    LOGGV("model path:%s\n", sz_model_path);
    LOGGV("prompt:%s\n", sz_prompt);
    LOGGV("bench type: %d\n", n_bench_type);
    LOGGV("thread counts:%d\n", n_thread_counts);
    LOGGV("backend type:%d\n", n_backend);

    if (0 == n_thread_counts)
        n_thread_counts = 1;

    result = llama_inference(sz_model_path, sz_prompt, n_bench_type, n_thread_counts, n_backend);
    LOGGD("result %d", result);

failure:
    if (NULL != sz_prompt) {
        (*env)->ReleaseStringUTFChars(env, prompt, sz_prompt);
    }

    if (NULL != sz_model_path) {
        (*env)->ReleaseStringUTFChars(env, model_path, sz_model_path);
    }

    jstring string = (*env)->NewStringUTF(env, sz_bench_result);

    return string;
}


void  ggml_jni_notify_c_impl(const char * format,  ...) {
    static unsigned char s_ggml_jni_buf[JNI_BUF_LEN];

    va_list va;
    int len_content = 0;

    memset(s_ggml_jni_buf, 0, JNI_BUF_LEN);

    va_start(va, format);

    len_content = vsnprintf(s_ggml_jni_buf, JNI_BUF_LEN, format, va);
    snprintf(s_ggml_jni_buf + len_content, JNI_BUF_LEN - len_content, "\n");

    kantv_asr_notify_benchmark_c(s_ggml_jni_buf);

    va_end(va);
}