package com.github.nhuray.dropwizard.spring;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class SecurityExpressionHandlerFix implements ServletContextListener {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(SecurityExpressionHandlerFix.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ApplicationContext webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(sce.getServletContext());
        ApplicationContext parentContext = webApplicationContext.getParent();
        String[] beanNamesForType = parentContext.getBeanNamesForType(SecurityExpressionHandler.class);

        try{
            for (String name : beanNamesForType) {
                ((AnnotationConfigWebApplicationContext)webApplicationContext).getBeanFactory().registerSingleton(name, parentContext.getBean(name));
            }
        } catch (Exception e){
            log.error("Could not fix security expression handler", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
