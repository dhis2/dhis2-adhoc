package org.hisp.dhis.adhoc.command;

import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyticsTablePopulator
{
    @Autowired
    private SchedulingManager schedulingManager;
    
    @Executed
    public void execute()
    {
        AnalyticsJobParameters analyticsJobParameters = new AnalyticsJobParameters();

        JobConfiguration analyticsTableJob = new JobConfiguration( "inMemoryAnalyticsJob", JobType.ANALYTICS_TABLE, "", analyticsJobParameters, false, true, true );

        schedulingManager.executeJob( analyticsTableJob );
    }
}
