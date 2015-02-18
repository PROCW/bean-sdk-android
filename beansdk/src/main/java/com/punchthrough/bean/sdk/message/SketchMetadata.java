/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Little Robots
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.punchthrough.bean.sdk.message;

import android.os.Parcelable;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.zip.CRC32;

import auto.parcel.AutoParcel;
import okio.Buffer;
import okio.ByteString;

import static com.punchthrough.bean.sdk.internal.utility.Misc.intToByte;
import static com.punchthrough.bean.sdk.internal.utility.Constants.MAX_SKETCH_NAME_LENGTH;

@AutoParcel
public abstract class SketchMetadata implements Parcelable {

    public abstract int hexSize();

    public abstract int hexCrc();

    public abstract Date timestamp();

    public abstract String hexName();

    public static SketchMetadata create(int hexSize, int hexCrc, Date timestamp, String hexName) {
        return new AutoParcel_SketchMetadata(hexSize, hexCrc, timestamp, hexName);
    }

    public static SketchMetadata create(SketchHex hex, Date timestamp) {
        int hexSize = hex.getBytes().length;
        String hexName = hex.getSketchName();

        CRC32 crc = new CRC32();
        crc.update(hex.getBytes());
        int hexCrc = (int) crc.getValue();

        return SketchMetadata.create(hexSize, hexCrc, timestamp, hexName);
    }

    public Buffer toPayload() {
        /* From AppMessages.h: BL_SKETCH_META_DATA_T struct
         *
         * {
         *   PTD_UINT32 hexSize;
         *   PTD_UINT32 hexCrc;
         *   PTD_UINT32 timestamp;
         *   PTD_UINT8 hexNameSize;
         *   PTD_UINT8 hexName[MAX_SKETCH_NAME_SIZE];
         * }
         */
        Buffer buffer = new Buffer();

        // Pad name to 20 bytes to fill buffer completely. It will be truncated by the Bean.
        String fullName = hexName();
        while (fullName.length() < MAX_SKETCH_NAME_LENGTH) {
            fullName += " ";
        }

        buffer.writeInt(hexSize());
        buffer.writeInt(hexCrc());
        buffer.writeInt((int) (new Date().getTime() / 1000));
        buffer.writeByte(intToByte(hexName().length()));
        buffer.write(ByteString.encodeUtf8(fullName));

        return buffer;
    }

    public static SketchMetadata fromPayload(Buffer buffer) {
        int hexSize = buffer.readIntLe();
        int hexCrc = buffer.readIntLe();
        long timestamp = (buffer.readIntLe() & 0xffffffffL) * 1000L;
        int hexNameSize = buffer.readByte() & 0xff;
        String hexName = "";
        if (hexNameSize > 0 && hexNameSize <= 20) {
            hexName = buffer.readString(hexNameSize, Charset.forName("UTF-8"));
        }
        return new AutoParcel_SketchMetadata(hexSize, hexCrc, new Date(timestamp), hexName);
    }

}
