# jersey-range-filter

Byte serving support for Jersey.

## Example usage

```java
import org.glassfish.jersey.server.ResourceConfig;
import javax.ws.rs.ApplicationPath;
import to.lova.jersey.filter.RangeResponseFilter;

@ApplicationPath("/app")
public class Application extends ResourceConfig {
  public Application() {
    this.register(RangeResponseFilter.class);
  }
}
```

Then Jersey will serve byte ranges whenever a HTTP `Range` header is present on
the request, e.g.

```
$ curl -i http://localhost:8080/app/alphabet
HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 26

abcdefghijklmnopqrstuvwxyz
```
```
$ curl -i http://localhost:8080/app/alphabet -H "Range: bytes=6-10"
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-Range: bytes 6-10/26
Content-Type: text/plain
Content-Length: 5

ghijk
```
```
$ curl -i http://localhost:8080/app/alphabet -H "Range: bytes=6-10,18-"
HTTP/1.1 206 Partial Content
Accept-Ranges: bytes
Content-Type: multipart/byteranges; boundary=VXTwimfDqIB3LRZ2jjQ8vxbSvnGAB2sMn3UVq
Content-Length: 257

--VXTwimfDqIB3LRZ2jjQ8vxbSvnGAB2sMn3UVq
Content-Type: text/plain
Content-Range: bytes 6-10/26

ghijk
--VXTwimfDqIB3LRZ2jjQ8vxbSvnGAB2sMn3UVq
Content-Type: text/plain
Content-Range: bytes 18-26/26

stuvwxyz
--VXTwimfDqIB3LRZ2jjQ8vxbSvnGAB2sMn3UVq--
```
