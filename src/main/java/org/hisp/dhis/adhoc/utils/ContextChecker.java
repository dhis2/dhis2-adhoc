package org.hisp.dhis.adhoc.utils;

import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests the Spring application context for circular dependencies.
 */
public class ContextChecker
{
    public static void main( String[] args )
    {
        String configLocation = "classpath*:/META-INF/dhis/beans.xml";
        
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext( configLocation );
        
        context.setAllowCircularReferences( false );
        context.refresh();
        context.close();
    }
}
