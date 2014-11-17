package com.gmsxo.domains.web.filter;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class AbstractFilter {
  private static final Logger LOG=Logger.getLogger(AbstractFilter.class);

	public AbstractFilter() {
		super();
	}

	protected void doLogin(ServletRequest request, ServletResponse response, HttpServletRequest req) throws ServletException, IOException {
	  LOG.debug("doLogin");
		RequestDispatcher rd = req.getRequestDispatcher("/pages/public/login.xhtml");
		rd.forward(request, response);
	}
	
	protected void accessDenied(ServletRequest request, ServletResponse response, HttpServletRequest req) throws ServletException, IOException {
	  LOG.debug("accessDenied");
		RequestDispatcher rd = req.getRequestDispatcher("/pages/public/accessDenied.xhtml");
		rd.forward(request, response);
	}
}