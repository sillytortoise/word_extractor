package com.extraction.steve;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.List;

public class InitDataListener implements InitializingBean, ServletContextAware {

    public static HashMap<String, List<String>> taskPool = null;

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void setServletContext(ServletContext servletContext) {

    }

}
