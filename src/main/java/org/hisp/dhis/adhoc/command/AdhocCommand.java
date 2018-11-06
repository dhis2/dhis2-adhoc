package org.hisp.dhis.adhoc.command;

import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class AdhocCommand
{
    @Autowired
    private DatabaseInfoProvider provider;
    
    @Autowired
    private SystemService systemService;

    @Transactional
    @Executed
    public void execute()
        throws Exception
    {
        DatabaseInfo dbInfo = provider.getDatabaseInfo();
        
        SystemInfo systemInfo = systemService.getSystemInfo();
        
        System.out.println( dbInfo );
        
        System.out.println( systemInfo.getDatabaseInfo() );
    }
}
