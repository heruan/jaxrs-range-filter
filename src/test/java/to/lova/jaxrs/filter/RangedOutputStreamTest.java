package to.lova.jaxrs.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RangedOutputStream}.
 */
public class RangedOutputStreamTest {

    /**
     * Tests a single range.
     */
    @Test
    public void singleRangeTest() {
        OutputStream baos = new ByteArrayOutputStream();
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        try (RangedOutputStream ros = new RangedOutputStream(baos, "bytes=6-10", "text/plain", headers)) {
            Assertions.assertEquals("bytes", ros.getAcceptRanges());
            Assertions.assertFalse(ros.isMultipart());
            PrintStream printStream = new PrintStream(ros);
            printStream.append("abcdefghijklmnopqrstuvwxyz");
            printStream.flush();
            Assertions.assertEquals("ghijk", baos.toString());
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

    /**
     * Tests multiple ranges.
     */
    @Test
    public void multiRangeTest() {
        OutputStream baos = new ByteArrayOutputStream();
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        try (RangedOutputStream ros = new RangedOutputStream(baos, "bytes=6-10,18-", "text/plain", headers)) {
            Assertions.assertEquals("bytes", ros.getAcceptRanges());
            Assertions.assertTrue(ros.isMultipart());
            String boundary = ros.getBoundary();
            String response =
            // @formatter:off
                    "--" + boundary + "\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Range: bytes 6-10/26\r\n" +
                    "\r\n" +
                    "ghijk\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Range: bytes 18-26/26\r\n" +
                    "\r\n" +
                    "stuvwxyz\r\n" +
                    "--" + boundary + "--";
            // @formatter:on
            PrintStream printStream = new PrintStream(ros);
            printStream.append("abcdefghijklmnopqrstuvwxyz");
            printStream.flush();
            Assertions.assertEquals(response, baos.toString());
        } catch (IOException e) {
            Assertions.fail(e);
        }
    }

}
