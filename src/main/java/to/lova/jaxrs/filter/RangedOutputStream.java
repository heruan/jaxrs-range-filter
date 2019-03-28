/*
 * Copyright 2017 Axians
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package to.lova.jaxrs.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * An {@link OutputStream} which support ranges.
 */
public class RangedOutputStream extends OutputStream {

    /**
     * A ranged {@link OutputStream}.
     */
    public class Range extends OutputStream {

        private final ByteArrayOutputStream outputStream;

        private final long from;

        private final long to;

        /**
         * Instantiates a new range.
         *
         * @param from
         *            the starting byte index
         * @param to
         *            the ending byte index
         */
        public Range(long from, long to) {
            this.outputStream = new ByteArrayOutputStream();
            this.from = from;
            this.to = to;
        }

        /**
         * Contains.
         *
         * @param index
         *            the index to check
         * @return true, if the index is in this range
         */
        public boolean contains(long index) {
            if (this.to == 0) {
                return (this.from <= index);
            }
            return (this.from <= index && index <= this.to);
        }

        /**
         * Returns the bytes of this range.
         *
         * @return the bytes of this range
         */
        public byte[] getBytes() {
            return this.outputStream.toByteArray();
        }

        /**
         * Returns the starting index of this range.
         *
         * @return the starting index of this range
         */
        public long getFrom() {
            return this.from;
        }

        /**
         * Returns the ending index of this range.
         *
         * @param ifZero
         *            the index to return if the range end is not bound
         * @return the ending index of this range
         */
        public Long getTo(long ifZero) {
            return this.to == 0 ? ifZero : this.to;
        }

        @Override
        public void write(int b) throws IOException {
            this.outputStream.write(b);
        }

    }

    private static final char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .toCharArray();

    private static final String BOUNDARY_LINE_FORMAT = "--%s";

    private static final String CONTENT_TYPE_LINE_FORMAT = "Content-Type: %s";

    private static final String CONTENT_RANGE_FORMAT = "%s %d-%d/%d";

    private static final String CONTENT_RANGE_LINE_FORMAT = "Content-Range: " + CONTENT_RANGE_FORMAT;

    private static final String EMPTY_LINE = "\r\n";

    private final OutputStream outputStream;

    private final String boundary;

    private final String accept;

    private final String contentType;

    private final boolean multipart;

    private boolean flushed = false;

    private long pos = 0;

    private final List<Range> ranges = new ArrayList<>();

    private final MultivaluedMap<String, Object> headers;

    /**
     * Instantiates a new ranged output stream.
     *
     * @param outputStream
     *            the output stream
     * @param ranges
     *            the ranges
     * @param contentType
     *            the content type
     * @param headers
     *            the headers
     */
    public RangedOutputStream(OutputStream outputStream, String ranges, String contentType,
            MultivaluedMap<String, Object> headers) {
        this.outputStream = outputStream;
        String[] acceptRanges = ranges.split("=");
        this.accept = acceptRanges[0];
        for (String range : acceptRanges[1].split(",")) {
            String[] bounds = range.split("-");
            this.ranges.add(new Range(Long.valueOf(bounds[0]), bounds.length == 2 ? Long.valueOf(bounds[1]) : 0));
        }
        this.headers = headers;
        this.contentType = contentType;
        this.multipart = this.ranges.size() > 1;
        this.boundary = this.generateBoundary();
    }

    private String generateBoundary() {
        StringBuilder buffer = new StringBuilder();
        Random rand = new Random();
        int count = rand.nextInt(11) + 30;
        for (int i = 0; i < count; i++) {
            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        return buffer.toString();
    }

    /**
     * Checks if is multipart.
     *
     * @return true, if is multipart
     */
    public boolean isMultipart() {
        return this.multipart;
    }

    /**
     * Returns the boundary.
     *
     * @return the boundary
     */
    public String getBoundary() {
        return this.boundary;
    }

    /**
     * Returns the accept ranges.
     *
     * @return the accept ranges
     */
    public String getAcceptRanges() {
        return this.accept;
    }

    @Override
    public void write(int b) throws IOException {
        for (Range range : this.ranges) {
            if (range.contains(this.pos)) {
                range.write(b);
            }
        }
        this.pos++;
    }

    @Override
    public void flush() throws IOException {
        if (this.flushed) {
            return;
        }
        if (this.multipart) {
            this.headers.putSingle(HttpHeaders.CONTENT_TYPE,
                    String.format("multipart/byteranges; boundary=%s", this.boundary));
            for (Range range : this.ranges) {
                this.outputStream.write(String.format(BOUNDARY_LINE_FORMAT.concat(EMPTY_LINE), this.boundary).getBytes());
                this.outputStream
                        .write(String.format(CONTENT_TYPE_LINE_FORMAT.concat(EMPTY_LINE), this.contentType).getBytes());
                this.outputStream.write(String.format(CONTENT_RANGE_LINE_FORMAT.concat(EMPTY_LINE), this.accept,
                        range.getFrom(), range.getTo(this.pos), this.pos).getBytes());
                this.outputStream.write(EMPTY_LINE.getBytes());
                this.outputStream.write(range.getBytes());
                this.outputStream.write(EMPTY_LINE.getBytes());
            }
            this.outputStream.write(String.format(BOUNDARY_LINE_FORMAT, this.boundary + "--").getBytes());
        } else {
            Range range = this.ranges.get(0);
            this.headers.putSingle("Content-Range",
                    String.format(CONTENT_RANGE_FORMAT, this.accept, range.getFrom(), range.getTo(this.pos), this.pos));
            this.outputStream.write(range.getBytes());
        }
        this.flushed = true;
    }

}
