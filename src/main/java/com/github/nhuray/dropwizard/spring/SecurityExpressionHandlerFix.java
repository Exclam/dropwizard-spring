package com.github.nhuray.dropwizard.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class SecurityExpressionHandlerFix implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ApplicationContext webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(sce.getServletContext());
        ApplicationContext parentContext = webApplicationContext.getParent();
        String[] beanNamesForType = parentContext.getBeanNamesForType(SecurityExpressionHandler.class);

        for (String name : beanNamesForType) {
            ((GenericApplicationContext)webApplicationContext).getBeanFactory().registerSingleton(name, parentContext.getBean(name));
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
