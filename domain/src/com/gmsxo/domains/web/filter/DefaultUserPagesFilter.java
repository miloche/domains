package com.gmsxo.domains.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.gmsxo.domains.data.User;

public class DefaultUserPagesFilter extends AbstractFilter implements Filter {
  private static final Logger LOG=Logger.getLogger(DefaultUserPagesFilter.class);
	@Override
	public void destroy() {
		LOG.debug("destroy");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
	  LOG.debug("doFilter");
		HttpServletRequest req = (HttpServletRequest) request;
		User user = (User) req.getSession(true).getAttribute("user");

		if(!user.isUser() && !user.isAdmin()){
			accessDenied(request, response, req);
			return;
		}

		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		LOG.debug("init");
	}
}