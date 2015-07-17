package com.simapi.server;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.*;

import com.simapi.config.Latency;
import com.simapi.config.Mapping;
import com.simapi.io.FileContentReader;
import io.netty.handler.codec.http.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.springframework.web.util.UriUtils;


/**
 * Netty channel handler to process request/response
 */
@Component
@Qualifier("servletNettyHandler")
@ChannelHandler.Sharable
public class ServletNettyHandler extends SimpleChannelInboundHandler<HttpRequest> {
    
    private static final Log LOGGER = LogFactory.getLog(ServletNettyHandler.class);
    
    private static final String JSON_MIME_TYPE = "application/json";
    private static final String XML_MIME_TYPE = "application/xml";
    
    private static final String JSON_FILENAME_SUFFIX = ".json";
    private static final String XML_FILENAME_SUFFIX = ".xml";
    
    private static final String DELAY_PARAM = "delay_ms";
    
    private static final int CHECK_AVAIL_MS = 50;
    
    @Autowired()
    @Qualifier("scheduledExecutorService")
    private ScheduledExecutorService scheduledExecutorService;
    
    @Autowired()
    @Qualifier("uriResolver")
    private UriResolver uriResolver;
    
    @Autowired
    @Qualifier("servletStats")
    private ServerStats serverStats;
    
    @Autowired
    @Qualifier("fileContentReader")
    private FileContentReader fileContentReader;
    
    private Random simpleRandom = new Random();
    
    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final HttpRequest request)
            throws Exception {
        
        if (!request.getDecoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }
        
        String uri = getUri(request);
        LOGGER.info("requested " + uri);
        
        final Mapping mapping = uriResolver.findUriMatchedMapping(uri);
        if (null == mapping) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }
        
        serverStats.recordRequest(mapping);
        
        long numRequestPerMin = serverStats.getNumRequestPerMin(mapping);
        if (numRequestPerMin > mapping.getThrouputInMin()
                && mapping.getRejectType() == Mapping.REJECT_TYPE.REJECT) {
            sendError(ctx, HttpResponseStatus.TOO_MANY_REQUESTS);
            return;
        }
        
        if (numRequestPerMin > mapping.getThrouputInMin()
                && mapping.getRejectType() == Mapping.REJECT_TYPE.WAIT) {
            LOGGER.info("num request per min > throuput in min: "
                    + mapping.getThrouputInMin()
                    + ", reject type is wait, will wait for available throuput");
            serviceResponseWhenThrouputAvailable(ctx, request, mapping);
        }
        
        if (numRequestPerMin <= mapping.getThrouputInMin()) {
            serviceResponse(ctx, request, mapping);
        }
    }
    
    private String getUri(HttpRequest request) {
        if (null == request) {
            throw new IllegalArgumentException("request can't be null");
        }
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(request.getUri()).build();
        return uriComponents.getPath();
    }
    
    private void serviceResponseWhenThrouputAvailable(final ChannelHandlerContext ctx,
            final HttpRequest request, final Mapping mapping) {
        if (null == mapping) {
            throw new IllegalArgumentException("mapping can't be null");
        }
        
        final Future<?>[] f = {
            null
        };
        f[0] = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            
            public void run() {
                if (serverStats.getNumRequestPerMin(mapping) <= mapping.getThrouputInMin()) {
                    serviceResponse(ctx, request, mapping);
                    
                    Future<?> future;
                    while (null == (future = f[0]))
                        Thread.yield();// prevent exceptionally bad thread
                    // scheduling
                    future.cancel(false);
                    return;
                    // cancel self
                }
            }
        }, 0, CHECK_AVAIL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void serviceResponse(final ChannelHandlerContext ctx,
            final HttpRequest request, final Mapping mapping) {
        if (null == mapping) {
            throw new IllegalArgumentException("mapping can't be null");
        }
        
        final String mimeType = getContentMimeType(request, mapping.getFileName());
        final String content = readContentFile(mapping.getFileName(), mimeType);
        Map<String, List<String>> requestParams = getQueryParams(request);
        
        long delay = getServiceDelay(mapping);
        LOGGER.info("specified latency: " + delay);
        
        List<String> delayValues = requestParams.get(DELAY_PARAM);
        if (!CollectionUtils.isEmpty(delayValues)) {
            try {
                long requestedDelay = Long.parseLong(delayValues.get(0));
                LOGGER.info("delay in request, ms: " + requestedDelay);
                delay += requestedDelay;
            }
            catch (NumberFormatException e) {
            }
        }
        if (delay > 0) {
            LOGGER.info("delaying api for ms: " + delay);
            ScheduledFuture scheduledFuture = scheduledExecutorService.schedule(
                    new Callable() {
                        
                        public Object call() throws Exception {
                            writeResponse2Channel(ctx, mimeType, request, content);
                            return null;
                        }
                    }, delay, TimeUnit.MILLISECONDS);
        }
        else {
            writeResponse2Channel(ctx, mimeType, request, content);
        }
    }
    
    private Map<String, List<String>> getQueryParams(final HttpRequest httpRequest) {
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(
                httpRequest.getUri()).build();
        Map<String, List<String>> result = new HashMap<>();
        
        try {
            for (Map.Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
                for (String value : entry.getValue()) {
                    String key = UriUtils.decode(entry.getKey(), "UTF-8");
                    String decodedValue = UriUtils.decode(value, "UTF-8");
                    
                    List<String> decodedValues = result.get(key);
                    if (null == decodedValues) {
                        decodedValues = new ArrayList<>();
                        result.put(key, decodedValues);
                    }
                    decodedValues.add(decodedValue);
                }
            }
        }
        catch (UnsupportedEncodingException ex) {
        }
        
        return result;
    }
    
    private String readContentFile(String fileName, String mimeType) {
        if (null == fileName || null == mimeType) {
            throw new IllegalArgumentException("filename or/and mimeType can't be null");
        }
        
        String content = "Mock Response not found";
        if (!fileName.endsWith(JSON_FILENAME_SUFFIX)
                && !fileName.endsWith(XML_FILENAME_SUFFIX)) {
            if (mimeType.equalsIgnoreCase(JSON_MIME_TYPE)) {
                fileName = fileName + JSON_FILENAME_SUFFIX;
            }
            else {
                fileName = fileName + XML_FILENAME_SUFFIX;
            }
        }
        try {
            content = fileContentReader.getFileContent(fileName);
        }
        catch (FileNotFoundException e) {
        }
        
        return content;
    }
    
    long getServiceDelay(Mapping mapping) {
        if (null == mapping) {
            throw new IllegalArgumentException("mapping can't be null");
        }
        
        long delay = 0;
        List<Latency> latencies = mapping.getLatencies();
        int sum = 0;
        for (Latency latency : latencies) {
            sum += latency.getPercent();
        }
        
        int p = simpleRandom.nextInt(sum);
        int cumulativeProbability = 0;
        for (Latency latency : latencies) {
            cumulativeProbability += latency.getPercent();
            if (p < cumulativeProbability) {
                delay = latency.getTimems();
                break;
            }
        }
        
        return delay;
    }
    
    String getContentMimeType(HttpRequest request, String fileName) {
        if (null == fileName || null == request) {
            throw new IllegalArgumentException("filename and/or request can't be empty");
        }
        
        String mimeType = XML_MIME_TYPE;
        if (fileName.contains(JSON_FILENAME_SUFFIX)) {
            mimeType = JSON_MIME_TYPE;
        }
        else if (fileName.contains(XML_FILENAME_SUFFIX)) {
            mimeType = XML_MIME_TYPE;
        }
        else {
            String contentType = request.headers().get(CONTENT_TYPE);
            if (contentType != null && contentType.contains(JSON_MIME_TYPE)) {
                mimeType = JSON_MIME_TYPE;
            }
        }
        
        return mimeType;
    }
    
    private void writeResponse2Channel(ChannelHandlerContext ctx, String mimeType,
            HttpRequest request, String content) {
        LOGGER.info("write response to channel");
        if (is100ContinueExpected(request)) {
            ctx.write(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE));
        }
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                HttpResponseStatus.OK, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, mimeType);
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
    
    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        String content = "Failure: " + status.toString() + "\r\n";
        
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().add(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response);
    }
}
