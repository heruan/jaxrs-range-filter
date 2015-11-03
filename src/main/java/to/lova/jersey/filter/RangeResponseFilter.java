package to.lova.jersey.filter;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response.Status;

/**
 * 
 * @author Giovanni Lovato <giovanni@lova.to>
 *
 */
public class RangeResponseFilter implements ContainerResponseFilter {

    private static final String RANGE = "Range";
    
    private static final String ACCEPT_RANGES = "Accept-Ranges";
    
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        if (requestContext.getHeaders().containsKey(RANGE)) {
            String rangeHeader = requestContext.getHeaderString(RANGE);
            String contentType = responseContext.getMediaType().toString();
            OutputStream originOutputStream = responseContext.getEntityStream();
            RangedOutputStream rangedOutputStream = new RangedOutputStream(originOutputStream, rangeHeader, contentType, responseContext.getHeaders());
            responseContext.setStatus(Status.PARTIAL_CONTENT.getStatusCode());
            responseContext.getHeaders().putSingle(ACCEPT_RANGES, rangedOutputStream.getAcceptRanges());
            responseContext.setEntityStream(rangedOutputStream);
        }
    }

}