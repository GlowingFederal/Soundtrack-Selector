package com.glowingfederal.soundtrackselector.domain;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Reads Vorbis identification packets and Ogg end granules, never decodes PCM. */
public final class OggDuration {
    private OggDuration() {}

    public static double read(InputStream input) throws IOException {
        byte[] header = new byte[27];
        byte[] lacing = new byte[255];
        byte[] identification = new byte[30];
        Map<Long, Stream> streams = new HashMap<Long, Stream>();
        while (true) {
            int first = input.read();
            if (first < 0) break;
            header[0] = (byte) first;
            readFully(input, header, 1, 26);
            if (header[0] != 'O' || header[1] != 'g' || header[2] != 'g' || header[3] != 'S' || header[4] != 0)
                throw new IOException("Invalid Ogg page header");
            long serial = little(header, 14, 4);
            int count = header[26] & 255;
            readFully(input, lacing, 0, count);
            int length = 0;
            for (int i = 0; i < count; i++) length += lacing[i] & 255;
            if ((header[5] & 2) != 0) {
                if (length < 30 || count == 0 || (lacing[0] & 255) < 30)
                    throw new IOException("Truncated identification packet");
                readFully(input, identification, 0, 30);
                length -= 30;
                if (identification[0] != 1 || identification[1] != 'v' || identification[2] != 'o'
                        || identification[3] != 'r' || identification[4] != 'b' || identification[5] != 'i'
                        || identification[6] != 's' || little(identification, 7, 4) != 0
                        || identification[11] == 0 || (identification[29] & 1) == 0)
                    throw new IOException("Unsupported or malformed audio codec (expected Vorbis)");
                long rate = little(identification, 12, 4);
                if (rate == 0 || rate > 768000 || streams.containsKey(serial))
                    throw new IOException("Invalid Vorbis stream");
                streams.put(serial, new Stream(rate));
            }
            skipFully(input, length);
            Stream stream = streams.get(serial);
            if (stream == null) throw new IOException("Ogg stream has no Vorbis identification packet");
            if ((header[5] & 4) != 0) {
                long granule = little(header, 6, 8);
                if (granule < 0 || stream.ended) throw new IOException("Invalid end granule");
                stream.samples = granule;
                stream.ended = true;
            }
        }
        if (streams.isEmpty()) throw new IOException("Empty Ogg resource");
        double duration = 0;
        for (Stream stream : streams.values()) {
            if (!stream.ended) throw new IOException("Ogg resource has no complete end page");
            duration += (double) stream.samples / stream.rate;
        }
        return duration;
    }

    private static long little(byte[] bytes, int start, int length) {
        long value = 0;
        for (int i = 0; i < length; i++) value |= (long) (bytes[start + i] & 255) << (8 * i);
        return value;
    }

    private static void readFully(InputStream input, byte[] bytes, int start, int length) throws IOException {
        while (length > 0) {
            int read = input.read(bytes, start, length);
            if (read < 0) throw new EOFException("Truncated Ogg resource");
            if (read == 0) {
                int value = input.read();
                if (value < 0) throw new EOFException("Truncated Ogg resource");
                bytes[start] = (byte) value;
                read = 1;
            }
            start += read;
            length -= read;
        }
    }

    private static void skipFully(InputStream input, int length) throws IOException {
        while (length > 0) {
            long skipped = input.skip(length);
            if (skipped == 0) {
                if (input.read() < 0) throw new EOFException("Truncated Ogg page body");
                skipped = 1;
            }
            length -= (int) skipped;
        }
    }

    private static final class Stream {
        final long rate;
        long samples;
        boolean ended;
        Stream(long rate) { this.rate = rate; }
    }
}
