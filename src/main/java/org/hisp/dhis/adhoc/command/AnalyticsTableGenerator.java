package org.hisp.dhis.adhoc.command;

import javax.annotation.Resource;

import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.analytics.table.scheduling.AnalyticsTableJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public class AnalyticsTableGenerator
{
    @Resource(name="analyticsAllTask")
    private AnalyticsTableJob job;
    
    @Autowired
    private Scheduler scheduler;
    
    @Executed
    public void execute()
    {
        AnalyticsJobParameters jobParameters = new AnalyticsJobParameters( null, Sets.newHashSet(), false );
        JobConfiguration jobConfiguration = new JobConfiguration( "AnalyticsTable", JobType.ANALYTICS_TABLE, "", jobParameters, false, true );
        
        scheduler.executeJob( () -> job.execute( jobConfiguration ) );
    }
}
