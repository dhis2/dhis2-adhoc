package org.hisp.dhis.adhoc.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.adhoc.annotation.Executed;
import org.springframework.transaction.annotation.Transactional;

public class ExampleCommand
{
    private static final Log log = LogFactory.getLog( ExampleCommand.class );
    
    @Executed
    @Transactional
    public void execute()
    {
        log.info( "Some stuff taking place here" );
    }
}
