package org.apache.velocity.tools.view.jsp.taglib.jspimpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.el.ELContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.ViewContext;

public class VelocityPageContext extends PageContext {

    private Context velocityContext;

    private Writer velocityWriter;

    private ViewContext viewContext;

    private JspWriter jspWriter;

    private HttpServletRequest request;

    private HttpServletResponse response;

    public VelocityPageContext(Context velocityContext, Writer velocityWriter, ViewContext viewContext)
    {
	    super();
	    this.velocityContext = velocityContext;
	    this.velocityWriter = velocityWriter;
	    this.viewContext = viewContext;
	    this.request = viewContext.getRequest();
	    this.response = viewContext.getResponse();
        jspWriter = new JspWriterImpl(velocityWriter);
        velocityContext.put("out", jspWriter);
    }

	@Override
    public void initialize(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL,
            boolean needsSession, int bufferSize, boolean autoFlush)
            throws IOException, IllegalStateException, IllegalArgumentException {
		// We never call it and we discard this object at the end of the page call.
    }

    @Override
    public void release() {
		// We never call it and we discard this object at the end of the page call.
    }

    @Override
    public HttpSession getSession() {
        return request.getSession(false);
    }

    @Override
    public Object getPage() {
        return viewContext;
    }

    @Override
    public ServletRequest getRequest() {
        return request;
    }

    @Override
    public ServletResponse getResponse() {
        return response;
    }

    @Override
    public Exception getException() {
        return null;
    }

    @Override
    public ServletConfig getServletConfig() {
        throw new UnsupportedOperationException("Servlet config is not supported");
    }

    @Override
    public ServletContext getServletContext() {
        return viewContext.getServletContext();
    }

    @Override
    public void forward(String relativeUrlPath) throws ServletException,
            IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(relativeUrlPath);
        if (dispatcher == null) {
            throw new IllegalArgumentException("Cannot forward to '" +relativeUrlPath + "'");
        }
        dispatcher.forward(request, new ExternalWriterHttpServletResponse(response, new PrintWriter(velocityWriter)));

    }

    @Override
    public void include(String relativeUrlPath) throws ServletException,
            IOException {
        include(relativeUrlPath, true);
    }

    @Override
    public void include(String relativeUrlPath, boolean flush)
            throws ServletException, IOException {
        if (flush) {
        	velocityWriter.flush();
        }
        RequestDispatcher dispatcher = request.getRequestDispatcher(relativeUrlPath);
        if (dispatcher == null) {
            throw new IllegalArgumentException("Cannot include '" +relativeUrlPath + "'");
        }
        dispatcher.include(request, new ExternalWriterHttpServletResponse(response, new PrintWriter(velocityWriter)));
    }

    @Override
    public void handlePageException(Exception e) throws ServletException,
            IOException {
    	handlePageException((Throwable) e);
    }

    @Override
    public void handlePageException(Throwable t) throws ServletException,
            IOException {
        if (t instanceof RuntimeException)
        {
	        throw (RuntimeException) t;
        }
        if (t instanceof Error)
        {
	        throw (Error) t;
        }
        throw new ServletException("Rethrowing unmanaged exception", t);
    }

    @Override
    public void setAttribute(String name, Object value) {
    	if (name == null) {
    		throw new NullPointerException("The attribute name is null");
    	}
        velocityContext.put(name, value);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
    	if (name == null) {
    		throw new NullPointerException("The attribute name is null");
    	}
        switch (scope)
        {
        case PAGE_SCOPE:
        	velocityContext.put(name, value);
	        break;
        case REQUEST_SCOPE:
        	request.setAttribute(name, value);
        	break;
        case SESSION_SCOPE:
        	if (value == null) {
        		HttpSession session = request.getSession(false);
        		if (session != null) {
        			session.removeAttribute(name);
        		}
        	} else {
        		HttpSession session = request.getSession();
        		session.setAttribute(name, value);
        	}
        	break;
        case APPLICATION_SCOPE:
        	viewContext.getServletContext().setAttribute(name, value);
        	break;
        default:
        	throw new IllegalArgumentException("Invalid scope constant value: " + scope);
        }
    }

    @Override
    public Object getAttribute(String name) {
    	if (name == null) {
    		throw new NullPointerException("The attribute name is null");
    	}
        return velocityContext.get(name);
    }

    @Override
    public Object getAttribute(String name, int scope) {
    	if (name == null) {
    		throw new NullPointerException("The attribute name is null");
    	}
        switch (scope)
        {
        case PAGE_SCOPE:
        	return velocityContext.get(name);
        case REQUEST_SCOPE:
        	return request.getAttribute(name);
        case SESSION_SCOPE:
    		HttpSession session = request.getSession(false);
    		if (session != null) {
    			return session.getAttribute(name);
    		} else {
    			return null;
    		}
        case APPLICATION_SCOPE:
        	return viewContext.getServletContext().getAttribute(name);
        default:
        	throw new IllegalArgumentException("Invalid scope constant value: " + scope);
        }
    }

    @Override
    public Object findAttribute(String name) {
    	if (name == null) {
    		throw new NullPointerException("The attribute name is null");
    	}
    	Object candidate = velocityContext.get(name);
    	if (candidate == null) {
    		candidate = request.getAttribute(name);
    		if (candidate == null) {
    			HttpSession session = request.getSession(false);
    			if (session != null) {
    				candidate = session.getAttribute(name);
    			}
    			if (candidate == null) {
    				candidate = viewContext.getServletContext().getAttribute(name);
    			}
    		}
    	}
        return candidate;
    }

    @Override
    public void removeAttribute(String name) {
    	if (name == null) {
    		throw new NullPointerException("The attribute name is null");
    	}
    	velocityContext.remove(name);
    	request.removeAttribute(name);
    	HttpSession session = request.getSession(false);
    	if (session != null) {
    		session.removeAttribute(name);
    	}
    	viewContext.getServletContext().removeAttribute(name);
    }

    @Override
    public void removeAttribute(String name, int scope) {
    	if (name == null) {
    		throw new NullPointerException("The attribute name is null");
    	}
        switch (scope)
        {
        case PAGE_SCOPE:
        	velocityContext.remove(name);
	        break;
        case REQUEST_SCOPE:
        	request.removeAttribute(name);
        	break;
        case SESSION_SCOPE:
    		HttpSession session = request.getSession(false);
    		if (session != null) {
    			session.removeAttribute(name);
    		}
        	break;
        case APPLICATION_SCOPE:
        	viewContext.getServletContext().removeAttribute(name);
        	break;
        default:
        	throw new IllegalArgumentException("Invalid scope constant value: " + scope);
        }
    }

    @Override
    public int getAttributesScope(String name) {
    	if (name == null) {
    		throw new NullPointerException("The attribute name is null");
    	}
    	if (velocityContext.get(name) != null) {
    		return PAGE_SCOPE;
    	}
		if (request.getAttribute(name) != null) {
			return REQUEST_SCOPE;
		}
		HttpSession session = request.getSession(false);
		if (session != null) {
			if (session.getAttribute(name) != null) {
				return SESSION_SCOPE;
			}
		}
		if (viewContext.getServletContext().getAttribute(name) != null) {
			return APPLICATION_SCOPE;
		}
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getAttributeNamesInScope(int scope) {
        switch (scope)
        {
        case PAGE_SCOPE:
        	return new Enumeration<String>()
			{

        		private int index = 0;

        		private Object[] keys = velocityContext.getKeys();

				public boolean hasMoreElements()
                {
	                return index < keys.length;
                }

				public String nextElement()
                {
	                String retValue = (String) keys[index];
	                index++;
	                return retValue;
                }
			};
        case REQUEST_SCOPE:
        	return request.getAttributeNames();
        case SESSION_SCOPE:
    		HttpSession session = request.getSession(false);
    		if (session != null) {
    			return session.getAttributeNames();
    		}
    		return new Enumeration<String>()
			{

				public boolean hasMoreElements()
                {
	                return false;
                }

				public String nextElement()
                {
	                throw new NoSuchElementException("This is an empty enumeration");
                }
			};
        case APPLICATION_SCOPE:
        	return viewContext.getServletContext().getAttributeNames();
        default:
        	throw new IllegalArgumentException("Invalid scope constant value: " + scope);
        }
    }

    @Override
    public JspWriter getOut() {
        return jspWriter;
    }

    @SuppressWarnings("deprecation")
    @Override
    public javax.servlet.jsp.el.ExpressionEvaluator getExpressionEvaluator() {
    	// Really, who cares?
    	throw new UnsupportedOperationException("This class works only with JSP 2.1");
    }

    @SuppressWarnings("deprecation")
    @Override
    public javax.servlet.jsp.el.VariableResolver getVariableResolver() {
    	// Really, who cares?
    	throw new UnsupportedOperationException("This class works only with JSP 2.1");
    }

    @Override
    public ELContext getELContext() {
    	// Really, who cares?
    	throw new UnsupportedOperationException("EL not available at this point");
    }
}
