package org.hisp.dhis.adhoc.utils;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests the Spring application context for circular dependencies.
 */
public class ContextChecker
{
    /**
     * Sets location of DHIS 2 home directory.
     */
    public static final String DHIS2_HOME = "/home/lars/dev/config/dhis"; // Change this to your environment
    
    public static void main( String[] args )
    {
        System.setProperty( "dhis2.home", DHIS2_HOME );
        
        String configLocation = "classpath*:/META-INF/dhis/beans.xml";
        
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext( configLocation );
        
        context.setAllowCircularReferences( false );
        context.refresh();
        context.close();
    }
}
