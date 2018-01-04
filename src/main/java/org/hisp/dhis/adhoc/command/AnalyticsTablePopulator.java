package org.hisp.dhis.adhoc.command;

import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyticsTablePopulator
{
    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;
    
    @Autowired
    private Scheduler scheduler;
    
    @Executed
    public void execute()
    {
        JobId jobId = new JobId( JobType.ANALYTICSTABLE_UPDATE, null );
        
        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
            .withTaskId( jobId )
            .build();

        scheduler.executeJob( () -> analyticsTableGenerator.generateTables( params ) );
    }
}
