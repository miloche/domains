package com.gmsxo.domains.helpers;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

public class Web {
  public static void setAttribute(String name, Object attr) {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    request.getSession().setAttribute(name, attr);
  }
  public static Object getAttribute(String name) {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    return request.getSession().getAttribute(name);
  }
  public static void removeAttribute(String name) {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    request.getSession().removeAttribute(name);
  }
  public static String getRequestParam(String name) {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    return request.getParameter(name);
  }
}
