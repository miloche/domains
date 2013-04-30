package com.gmsxo.httpclient;

import java.io.IOException;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.Header;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

public class TryIt {
  private static final Logger LOG=Logger.getLogger(TryIt.class);
  /**
   * @param args
   * @throws IOException 
   * @throws HttpException 
   */
  public static void main(String[] args) throws IOException, HttpException {
    //HttpHost target = new HttpHost("velascodesign.com", 80, "http");
    HttpHost target = new HttpHost("72.167.131.131", 80, "http");
    

    // some general setup
    // Register the "http" protocol scheme, it is required
    // by the default operator to look up socket factories.
    SchemeRegistry supportedSchemes = new SchemeRegistry();
    supportedSchemes.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

    // Prepare parameters.
    // Since this example doesn't use the full core framework,
    // only few parameters are actually required.
    HttpParams params = new SyncBasicHttpParams();
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    HttpProtocolParams.setUseExpectContinue(params, false);

    // one operator can be used for many connections
    ClientConnectionOperator scop = new DefaultClientConnectionOperator(supportedSchemes);

    //HttpRequest req = new BasicHttpRequest("OPTIONS", "*", HttpVersion.HTTP_1_1);
    HttpRequest req = new BasicHttpRequest("GET", "/", HttpVersion.HTTP_1_1);
    req.addHeader("Host", target.getHostName());

    HttpContext ctx = new BasicHttpContext();

    OperatedClientConnection conn = scop.createConnection();
    try {
        System.out.println("opening connection to " + target);
        scop.openConnection(conn, target, null, ctx, params);
        System.out.println("sending request");
        conn.sendRequestHeader(req);
        // there is no request entity
        conn.flush();

        System.out.println("receiving response header");
        HttpResponse rsp = conn.receiveResponseHeader();
        
        System.out.println(conn.getRemoteAddress().getHostName());

        System.out.println("----------------------------------------");
        System.out.println(rsp.getStatusLine());
        Header[] headers = rsp.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            System.out.println(headers[i]);
        }
        System.out.println("----------------------------------------");
    } finally {
        System.out.println("closing connection");
        conn.close();
    }

  }

}
