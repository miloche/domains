package com.gmsxo.domains.web.rewriter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class UrlRewriteFilter implements Filter {
  private static Logger LOG=Logger.getLogger(UrlRewriteFilter.class);
  private static final Pattern PATTERN_DOMAIN=Pattern.compile("/domain/domain/(.*?)$");
  private static final Pattern PATTERN_IP=Pattern.compile("/domain/ip/(.*?)$");
  private static final Pattern PATTERN_DNS=Pattern.compile("/domain/dns/(.*?)$");

  public UrlRewriteFilter () {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
          FilterChain chain)
          throws IOException, ServletException {
      LOG.debug("doFilter()");
      HttpServletRequest srequest = (HttpServletRequest) request;

      String url = srequest.getRequestURI().trim();
      
      LOG.debug("doFilter url "+url);
      LOG.debug("doFilete srequest.getQueryString() "+srequest.getQueryString());
      // Process the written URL in the form
      // http://tld.com/myapp/store/specials/value_item_description
      // Forward to the original page silently without
      // the knowledge of the browser, URL displayed in
      // browser remains the same.
      //
      // In our case, before we forward the page
      // we first make sure the descriptive text part
      // of the URL was not changed by comparing it
      // against the database record and 
      // do a redirect if it was changed and then 
      // takes us back here eventually.
      Matcher matcher;
      if ((matcher=PATTERN_DOMAIN.matcher(url)).find()) {
          StringBuilder forward = new StringBuilder();                        
          forward.append("/pages/public/domain.xhtml?domain=");
          forward.append(matcher.group(1));
          request.getRequestDispatcher(forward.toString()).forward(request, response);
           
      // Process access to the original URL in the form 
      // http://tld.com/myapp/store_page.xhtml?item=value&subitem=value2  
      // This takes another loop to this filter but to the if part of this
      // block.  The browser is aware of the redirect and URL displayed
      // in the browser is rewritten.
           
      } else if ((matcher=PATTERN_IP.matcher(url)).find()) {
        LOG.debug("forward: ip.xhtml");
        StringBuilder forward = new StringBuilder();                        
        forward.append("/pages/public/ip.xhtml?ip=");
        LOG.debug("forward: srequest.getQueryString()");
        forward.append(matcher.group(1));
        request.getRequestDispatcher(forward.toString()).forward(request, response);
      } 
      else if (url.contains("/domain/dns/")) {
        StringBuilder forward = new StringBuilder();                        
        forward.append("/pages/public/dns.xhtml?");
        forward.append(matcher.group(1));
        request.getRequestDispatcher(forward.toString()).forward(request, response);
      }
      else {            
        chain.doFilter(request, response);
      }
  }
   
  private String rebuildUrl (HttpServletRequest srequest) {
       
      final String itemValue = srequest.getParameter("item");
      final String subItemValue = srequest.getParameter("sub_item");        
      String description = "retrieve value from database or some where else";
       
      // In our case, we included itemValue to the rewritten
      // URL because itemValue is the key to our database. We retrieve 
      // page data based on itemValue.

      description = itemValue+"-"+
        subItemValue+"-" +
            description.replaceAll("[^a-z0-9]", "_");
       
      // replaceAll above replaces all non alpha-numeric characters in
      // the description with an underscore.  
              
      return description;
  }


  @Override
  public void destroy() {
      // your code
  }

  @Override
  public void init(FilterConfig filterConfig) { 
      // your code
  }

}
