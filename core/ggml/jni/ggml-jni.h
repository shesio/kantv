/*
 * Copyright (c) 2024, zhou.weiguo(zhouwg2000@gmail.com)
 *
 * Copyright (c) 2024- KanTV Authors
 *
 * this clean-room implementation is for
 *
 * PoC(https://github.com/zhouwg/kantv/issues/64) in project KanTV. the initial implementation was done
 *
 * from 03-05-2024 to 03-16-2024.the initial implementation could be found at:
 *
 * https://github.com/zhouwg/kantv/blob/kantv-poc-with-whispercpp/external/whispercpp/whisper.cpp#L6727

 * https://github.com/zhouwg/kantv/blob/kantv-poc-with-whispercpp/external/whispercpp/whisper.h#L620

 * https://github.com/zhouwg/kantv/blob/kantv-poc-with-whispercpp/external/whispercpp/jni/whispercpp-jni.c

 * https://github.com/zhouwg/kantv/blob/kantv-poc-with-whispercpp/cdeosplayer/cdeosplayer-lib/src/main/java/org/ggml/whispercpp/whispercpp.java
 *
 *
 * in short, it's a very concise implementation and the method here is never seen in any other similar
 *
 * (whisper.cpp related) open-source project before 03-05-2024.
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
 * The above statement and notice must be included in corresponding files in derived project
 */

#ifndef KANTV_GGML_JNI_H
#define KANTV_GGML_JNI_H

#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>
#include "libavutil/cde_log.h"
#include "ggml.h"

#ifdef __cplusplus
extern "C" {
#endif

#define JNI_BUF_LEN                 4096
#define JNI_TMP_LEN                 256

#define BECHMARK_ASR                0
#define BECHMARK_MEMCPY             1
#define BECHMARK_MULMAT             2
#define BECHMARK_FULL               3
#define BENCHMARK_MATRIX            4
#define BENCHMAKR_LLAMA             5
#define BENCHMAKR_QNN_SAMPLE        6
#define BENCHMAKR_QNN_SAVER         7
#define BENCHMARK_QNN_MATRIX        8
#define BENCHMARK_QNN_GGML          9
#define BENCHMAKR_STABLEDIFFUSION   10
#define BENCHMAKR_MAX               10

#define BACKEND_CPU                 0
#define BACKEND_GPU                 1
#define BACKEND_DSP                 2
#define BACKEND_SAVER               3
#define BACKEND_MAX                 3

    enum ggml_jni_op {
        GGML_JNI_OP_NONE = 0,
        GGML_JNI_OP_ADD,
        GGML_JNI_OP_SUB,
        GGML_JNI_OP_MUL,
        GGML_JNI_OP_DIV,
        GGML_JNI_OP_SUM,
        GGML_JNI_OP_MUL_MAT
    };

#define GGML_JNI_NOTIFY(...)        ggml_jni_notify_c_impl(__VA_ARGS__)

// JNI helper function for whisper.cpp benchmark
    void         ggml_jni_notify_c_impl(const char * format, ...);
    int          whisper_get_cpu_core_counts(void);
    void         whisper_set_benchmark_status(int b_exit_benchmark);
    /**
    *
    * @param sz_model_path         /sdcard/kantv/ggml-xxxxxx.bin or  /sdcard/kantv/xxxxxx.gguf or qualcomm's dedicated model
    * @param sz_audio_path         /sdcard/kantv/jfk.wav
    * @param n_bench_type          0: asr(transcription) 1: memcpy 2: mulmat  3: full/whisper_encode 4: matrix  5: LLAMA 6: QNN sample 7: QNN saver 8: QNN matrix 9: QNN GGML 10: stable diffusion
    * @param n_threads             1 - 8
    * @param n_backend_type        0: CPU  1: GPU  2: DSP
    * @param n_op_type             type of matrix manipulate / GGML OP
    * @return
    */
    // renamed to ggml_jni_bench for purpose of unify JNI layer of whisper.cpp and llama.cpp
    // and QNN(Qualcomm Neural Network, aka Qualcomm AI Engine Direct) SDK
    void         ggml_jni_bench(const char *model_path, const char *audio_path, int n_bench_type, int num_threads, int n_backend_type, int n_op_type);


    const char * whisper_get_ggml_type_str(enum ggml_type wtype);


    // JNI helper function for ASR(whisper.cpp)
    /**
    * @param sz_model_path
    * @param n_threads
    * @param n_asrmode            0: normal transcription  1: asr pressure test 2:benchmark 3: transcription + audio record
    */
    int          whisper_asr_init(const char *sz_model_path, int n_threads, int n_asrmode);
    void         whisper_asr_finalize(void);

    void         whisper_asr_start(void);
    void         whisper_asr_stop(void);
    /**
    * @param sz_model_path
    * @param n_threads
    * @param n_asrmode            0: normal transcription  1: asr pressure test 2:benchmark 3: transcription + audio record
    */
    int          whisper_asr_reset(const char * sz_model_path, int n_threads, int n_asrmode);





// =================================================================================================
// trying to integrate llama.cpp from 03/26/2024 to 03/28/2024
// =================================================================================================


    // JNI helper function for llama.cpp
    /**
    *
    * @param sz_model_path         /sdcard/kantv/xxxxxx.gguf
    * @param prompt
    * @param bench_type            not used currently
    * @param n_threads             1 - 8
    * @return
    */
    int          llama_inference(const char * model_path, const char * prompt, int bench_type, int num_threads);

    void         ggml_bench_matrix(int num_threads);

    int          ggml_bench_llama(const char * sz_model_path, int num_threads);




// =================================================================================================
// PoC#121:Add Qualcomm mobile SoC native backend for GGML from 03-29-2024
// =================================================================================================

    int qnn_sample_main(int argc, char** argv);

    int qnn_saver_main(int argc, char** argv);

    int qnn_matrix(int n_backend_type, int n_op_type);

    int qnn_ggml(int n_backend_type, int n_ggml_op_type);




// =================================================================================================
// trying to integrate stablediffusion.cpp on 04-06-2024(Apri,6,2024)
// =================================================================================================


// JNI helper function for stablediffusion.cpp
/**
*
* @param sz_model_path         /sdcard/kantv/xxxxxx.gguf
* @param prompt
* @param bench_type            not used currently
* @param n_threads             1 - 8
* @return
*/
int          stablediffusion_inference(const char * model_path, const char * prompt, int bench_type, int num_threads);


#ifdef __cplusplus
}
#endif


#endif