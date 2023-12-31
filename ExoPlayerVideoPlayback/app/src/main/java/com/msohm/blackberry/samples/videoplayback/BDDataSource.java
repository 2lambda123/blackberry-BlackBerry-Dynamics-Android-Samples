/* Some modifications (c) BlackBerry Limited 2021 adapted from
* library/core/src/main/java/com/google/android/exoplayer2/upstream/FileDataSource.java
* from https://github.com/google/ExoPlayer
* and licensed same as follows
*
* Copyright (C) 2016 The Android Open Source Project
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
 

package com.msohm.blackberry.samples.videoplayback;

import static com.google.android.exoplayer2.util.Util.castNonNull;
import static java.lang.Math.min;

import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.BaseDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.good.gd.file.RandomAccessFile;
import com.good.gd.file.File;

/** A {@link DataSource} for reading local files. */
public final class BDDataSource extends BaseDataSource {

    /** Thrown when a {@link BDDataSource} encounters an error reading a file. */
    public static class BDDataSourceException extends IOException {

        public BDDataSourceException(IOException cause) {
            super(cause);
        }

        public BDDataSourceException(String message, IOException cause) {
            super(message, cause);
        }
    }

    /** {@link DataSource.Factory} for {@link BDDataSource} instances. */
    public static final class Factory implements DataSource.Factory {

        @Nullable private TransferListener listener;

        /**
         * Sets a {@link TransferListener} for {@link BDDataSource} instances created by this factory.
         *
         * @param listener The {@link TransferListener}.
         * @return This factory.
         */
        public Factory setListener(@Nullable TransferListener listener) {
            this.listener = listener;
            return this;
        }

        @Override
        public BDDataSource createDataSource() {
            BDDataSource dataSource = new BDDataSource();
            if (listener != null) {
                dataSource.addTransferListener(listener);
            }
            return dataSource;
        }
    }

    @Nullable private RandomAccessFile file;
    @Nullable private Uri uri;
    private long bytesRemaining;
    private boolean opened;

    public BDDataSource() {
        super(/* isNetwork= */ false);
    }

    @Override
    public long open(DataSpec dataSpec) throws BDDataSourceException {
        try {
            Uri uri = dataSpec.uri;
            this.uri = uri;

            transferInitializing(dataSpec);

            this.file = openLocalFile(uri);
            file.seek(dataSpec.position);
            bytesRemaining = dataSpec.length == C.LENGTH_UNSET ? file.length() - dataSpec.position
                    : dataSpec.length;
            if (bytesRemaining < 0) {
                throw new EOFException();
            }
        } catch (IOException e) {
            throw new BDDataSourceException(e);
        }

        opened = true;
        transferStarted(dataSpec);

        return bytesRemaining;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws BDDataSourceException {
        if (readLength == 0) {
            return 0;
        } else if (bytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        } else {
            int bytesRead;
            try {
                bytesRead = castNonNull(file).read(buffer, offset, (int) min(bytesRemaining, readLength));
            } catch (IOException e) {
                throw new BDDataSourceException(e);
            }

            if (bytesRead > 0) {
                bytesRemaining -= bytesRead;
                bytesTransferred(bytesRead);
            }

            return bytesRead;
        }
    }

    @Override
    @Nullable
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close() {
        uri = null;
        if (file != null) {
            file.close();
        }
        file = null;
        if (opened) {
            opened = false;
            transferEnded();
        }
    }

    private static RandomAccessFile openLocalFile(Uri uri) throws BDDataSourceException {
        try {
            File file = new File(Assertions.checkNotNull(uri.getPath()));
            return new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            if (!TextUtils.isEmpty(uri.getQuery()) || !TextUtils.isEmpty(uri.getFragment())) {
                throw new BDDataSourceException(
                        String.format(
                                "uri has query and/or fragment, which are not supported. Did you call Uri.parse()"
                                        + " on a string containing '?' or '#'? Use Uri.fromFile(new File(path)) to"
                                        + " avoid this. path=%s,query=%s,fragment=%s",
                                uri.getPath(), uri.getQuery(), uri.getFragment()),
                        e);
            }
            throw new BDDataSourceException(e);
        }
    }
}