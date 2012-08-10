
package com.pekall.pctool;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import android.content.UriMatcher;
import android.net.Uri;

import com.example.tutorial.AddressBookProtos.AddressBook;
import com.pekall.pctool.model.FakeBusinessLogicFacade;
import com.pekall.pctool.protos.AppInfoProtos.AppInfoPList;
import com.pekall.pctool.protos.MsgDefProtos.AppRecord;
import com.pekall.pctool.protos.MsgDefProtos.CmdRequest;
import com.pekall.pctool.protos.MsgDefProtos.CmdResponse;
import com.pekall.pctool.protos.MsgDefProtos.CmdType;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class MainServerHandler extends SimpleChannelUpstreamHandler {

    private static final int RPC_END_POINT = 1;
    private static final int APPS = 2;
    private static final int TEST = 3;

    private static final UriMatcher sURIMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI("localhost", "rpc", RPC_END_POINT);
        sURIMatcher.addURI("localhost", "apps", APPS);
        sURIMatcher.addURI("localhost", "test", TEST);
    }

    private FakeBusinessLogicFacade mLogicFacade;

    public MainServerHandler(FakeBusinessLogicFacade facade) {
        this.mLogicFacade = facade;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();

        String path = request.getUri();
        HttpMethod method = request.getMethod();

        path = sanitizeUri(path);

        Slog.d("path:" + path + ", method: " + method);

        Uri url = Uri.parse("content://localhost" + path);

        int match = sURIMatcher.match(url);

        Slog.d("url = " + url);
        Slog.d("match = " + match);

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);

        switch (match) {
            case RPC_END_POINT: {
                if (HttpMethod.POST.equals(method)) {
                    handleRpc(request, response);
                } else {
                    Slog.e("not http post request");
                }
                break;
            }

            case APPS: {
                handleApps(request, response);
                break;
            }
            case TEST: {
                handleTest(request, response);
                break;
            }
            default: {
                handleNotFound(request, response);
                break;
            }
        }

        Channel ch = e.getChannel();
        // Write the initial line and the header.
        ChannelFuture future = ch.write(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    private void handleRpc(HttpRequest request, HttpResponse response) {
        
        Slog.d("handleRpc");
        
        ChannelBuffer content = request.getContent();
        
        ChannelBufferInputStream cbis = new ChannelBufferInputStream(content);
        
        try {
            CmdRequest cmdRequest = CmdRequest.parseFrom(cbis);
            CmdType cmdType = cmdRequest.getType();
            
            Slog.d("cmdType = " + cmdType);
            switch (cmdType) {
                case CMD_QUERY_APP:
                    AppRecord appRecord = cmdRequest.getAppParams();
                    if (appRecord != null) {
                        Slog.d("type = " + appRecord.getType());
                        Slog.d("location = " + appRecord.getLocation());
                    }
                    
                    CmdResponse cmdResponse = mLogicFacade.queryAppRecordList();
                    
                    ChannelBuffer buffer = new DynamicChannelBuffer(2048);
                    buffer.writeBytes(cmdResponse.toByteArray());

                    response.setContent(buffer);
                    response.setHeader(CONTENT_TYPE, "application/x-protobuf");
                    response.setHeader(CONTENT_LENGTH, response.getContent().writerIndex());
                    break;

                default:
                    break;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void handleApps(HttpRequest request, HttpResponse response) {
        AppInfoPList appInfoPList = mLogicFacade.getAppInfoPList();

        ChannelBuffer buffer = new DynamicChannelBuffer(2048);
        buffer.writeBytes(appInfoPList.toByteArray());

        response.setContent(buffer);
        response.setHeader(CONTENT_TYPE, "application/x-protobuf");
        response.setHeader(CONTENT_LENGTH, response.getContent().writerIndex());
    }

    private void handleTest(HttpRequest request, HttpResponse response) {
        AddressBook addressBook = mLogicFacade.getAddressBook();

        ChannelBuffer buffer = new DynamicChannelBuffer(2048);
        buffer.writeBytes(addressBook.toByteArray());

        response.setContent(buffer);
        response.setHeader(CONTENT_TYPE, "application/x-protobuf");
        response.setHeader(CONTENT_LENGTH, response.getContent().writerIndex());
    }

    private void handleNotFound(HttpRequest request, HttpResponse response) {
        response.setStatus(NOT_FOUND);
    }

    private static String sanitizeUri(String uri) {
        // Decode the path.
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException e1) {
                throw new Error();
            }
        }
        return uri;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        Channel ch = e.getChannel();
        Throwable cause = e.getCause();
        if (cause instanceof TooLongFrameException) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        cause.printStackTrace();
        if (ch.isConnected()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer(
                "Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response)
                .addListener(ChannelFutureListener.CLOSE);
    }
}
