/*
 * customized CryptoInfo which borrow from Google's ExoPlayer
 * Copyright (c) Project KanTV. 2021-2023. All rights reserved.
 *
 * Copyright (c) 2024- KanTV Authors. All Rights Reserved.
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
 */
package cdeos.media.player;

import android.media.MediaCodec;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Compatibility wrapper for {@link android.media.MediaCodec.CryptoInfo}. */
public class CDECryptoInfo {
    private static volatile boolean mIsInit = false;
    private static CDECryptoInfo instance = null;
    public int isEncrypted;
    public int dataLength;
    public byte[] data;
    /**
     * The 16 byte initialization vector. If the initialization vector of the content is shorter than
     * 16 bytes, 0 byte padding is appended to extend the vector to the required 16 byte length.
     *
     * @see android.media.MediaCodec.CryptoInfo#iv
     */
    @Nullable
    public byte[] iv;
    /**
     * The 16 byte key id.
     *
     * @see android.media.MediaCodec.CryptoInfo#key
     */
    @Nullable public byte[] key;
    /**
     * The type of encryption that has been applied. Must be one of the following values;
    public static final int CRYPTO_MODE_UNENCRYPTED     = MediaCodec.CRYPTO_MODE_UNENCRYPTED; //0
    public static final int CRYPTO_MODE_AES_CTR         = MediaCodec.CRYPTO_MODE_AES_CTR;     //1
    public static final int CRYPTO_MODE_AES_CBC         = MediaCodec.CRYPTO_MODE_AES_CBC;     //2
    //weiguo: make native multidrm & wiseplay happy
    public static final int CRYPTO_MODE_SM4_CTR         = 3;
    public static final int CRYPTO_MODE_SM4_CBC         = 4;
     */
    @Nullable public int mode;
    /**
     * The number of leading unencrypted bytes in each sub-sample. If null, all bytes are treated as
     * encrypted and {@link #numBytesOfEncryptedData} must be specified.
     *
     * @see android.media.MediaCodec.CryptoInfo#numBytesOfClearData
     */
    @Nullable public int[] numBytesOfClearData;
    /**
     * The number of trailing encrypted bytes in each sub-sample. If null, all bytes are treated as
     * clear and {@link #numBytesOfClearData} must be specified.
     *
     * @see android.media.MediaCodec.CryptoInfo#numBytesOfEncryptedData
     */
    @Nullable public int[] numBytesOfEncryptedData;
    /**
     * The number of subSamples that make up the buffer's contents.
     *
     * @see android.media.MediaCodec.CryptoInfo#numSubSamples
     */
    public int numSubSamples;
    /** @see android.media.MediaCodec.CryptoInfo.Pattern */
    public int encryptedBlocks;
    /** @see android.media.MediaCodec.CryptoInfo.Pattern */
    public int clearBlocks;

    private final android.media.MediaCodec.CryptoInfo frameworkCryptoInfo;
    @Nullable private final PatternHolderV24 patternHolder;

    private CDECryptoInfo() {
        frameworkCryptoInfo = new android.media.MediaCodec.CryptoInfo();
        patternHolder = android.os.Build.VERSION.SDK_INT  >= 24 ? new PatternHolderV24(frameworkCryptoInfo) : null;
    }
    public static CDECryptoInfo getInstance() {
        if (!mIsInit) {
            instance = new CDECryptoInfo();
            mIsInit = true;
        }
        return instance;
    }

    /** @see android.media.MediaCodec.CryptoInfo#set(int, int[], int[], byte[], byte[], int) */
    public void setCryptoInfo(
            int isEncrypted, int dataLength, byte[] data,
            int numSubSamples,
            int[] numBytesOfClearData,
            int[] numBytesOfEncryptedData,
            byte[] key,
            byte[] iv,
            int mode,
            int encryptedBlocks,
            int clearBlocks) {
        this.isEncrypted                            = isEncrypted;
        this.dataLength                             = dataLength;
        this.data                                   = data;
        this.numSubSamples                          = numSubSamples;
        this.numBytesOfClearData                    = numBytesOfClearData;
        this.numBytesOfEncryptedData                = numBytesOfEncryptedData;
        this.key                                    = key;
        this.iv                                     = iv;
        this.mode                                   = mode;
        this.encryptedBlocks                        = encryptedBlocks;
        this.clearBlocks                            = clearBlocks;
        frameworkCryptoInfo.numSubSamples           = numSubSamples;
        frameworkCryptoInfo.numBytesOfClearData     = numBytesOfClearData;
        frameworkCryptoInfo.numBytesOfEncryptedData = numBytesOfEncryptedData;
        frameworkCryptoInfo.key                     = key;
        frameworkCryptoInfo.iv                      = iv;
        frameworkCryptoInfo.mode                    = mode;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            if (patternHolder != null) {
                patternHolder.set(encryptedBlocks, clearBlocks);
            }
        }
    }

    /**
     * Returns an equivalent {@link android.media.MediaCodec.CryptoInfo} instance.
     *
     * <p>Successive calls to this method on a single {@link CDECryptoInfo} will return the same
     * instance. Changes to the {@link CDECryptoInfo} will be reflected in the returned object. The
     * return object should not be modified directly.
     *
     * @return The equivalent {@link android.media.MediaCodec.CryptoInfo} instance.
     */
    public android.media.MediaCodec.CryptoInfo getFrameworkCryptoInfo() {
        return frameworkCryptoInfo;
    }

    /**
     * Increases the number of clear data for the first sub sample by {@code count}.
     *
     * <p>If {@code count} is 0, this method is a no-op. Otherwise, it adds {@code count} to {@link
     * #numBytesOfClearData}[0].
     *
     * <p>If {@link #numBytesOfClearData} is null (which is permitted), this method will instantiate
     * it to a new {@code int[1]}.
     *
     * @param count The number of bytes to be added to the first subSample of {@link
     *     #numBytesOfClearData}.
     */
    public void increaseClearDataFirstSubSampleBy(int count) {
        if (count == 0) {
            return;
        }
        if (numBytesOfClearData == null) {
            numBytesOfClearData = new int[1];
            frameworkCryptoInfo.numBytesOfClearData = numBytesOfClearData;
        }
        numBytesOfClearData[0] += count;
    }

    @RequiresApi(24)
    private static final class PatternHolderV24 {

        private final android.media.MediaCodec.CryptoInfo frameworkCryptoInfo;
        private final android.media.MediaCodec.CryptoInfo.Pattern pattern;

        private PatternHolderV24(android.media.MediaCodec.CryptoInfo frameworkCryptoInfo) {
            this.frameworkCryptoInfo = frameworkCryptoInfo;
            pattern = new android.media.MediaCodec.CryptoInfo.Pattern(0, 0);
        }

        private void set(int encryptedBlocks, int clearBlocks) {
            pattern.set(encryptedBlocks, clearBlocks);
            frameworkCryptoInfo.setPattern(pattern);
        }
    }

    public static native int kantv_anti_remove_rename_this_file();
}

