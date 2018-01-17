package to.lova.jaxrs.filter;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

/**
 * A {@link ContainerResponseFilter} capable to handle ranged requests.
 */
public class RangeResponseFilter implements ContainerResponseFilter {

    private static final String ACCEPT_RANGES = "Accept-Ranges";

    private static final String BYTES_RANGE = "bytes";

    private static final String RANGE = "Range";

    private static final String IF_RANGE = "If-Range";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        responseContext.getHeaders().add(ACCEPT_RANGES, BYTES_RANGE);

        if (!requestContext.getHeaders().containsKey(RANGE)) {
            return;
        } else if (requestContext.getHeaders().containsKey(IF_RANGE)) {
            String ifRangeHeader = requestContext.getHeaderString(IF_RANGE);
            if (responseContext.getHeaders().containsKey(HttpHeaders.ETAG)
                    && responseContext.getHeaderString(HttpHeaders.ETAG).equals(ifRangeHeader)) {
                this.applyFilter(requestContext, responseContext);
                return;
            }
            if (responseContext.getHeaders().containsKey(HttpHeaders.LAST_MODIFIED)
                    && responseContext.getHeaderString(HttpHeaders.LAST_MODIFIED).equals(ifRangeHeader)) {
                this.applyFilter(requestContext, responseContext);
                return;
            }
        } else {
            this.applyFilter(requestContext, responseContext);
        }
    }

    private void applyFilter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {

        String rangeHeader = requestContext.getHeaderString(RANGE);
        String contentType = responseContext.getMediaType().toString();
        OutputStream originOutputStream = responseContext.getEntityStream();
        RangedOutputStream rangedOutputStream = new RangedOutputStream(originOutputStream, rangeHeader, contentType,
                responseContext.getHeaders());
        responseContext.setStatus(Status.PARTIAL_CONTENT.getStatusCode());
        responseContext.setEntityStream(rangedOutputStream);

    }

}
