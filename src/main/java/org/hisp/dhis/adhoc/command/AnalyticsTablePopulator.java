package org.hisp.dhis.adhoc.command;

import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyticsTablePopulator
{
    @Autowired
    private AnalyticsTableGenerator generator;

    @Executed
    public void execute()
    {
        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder()
            .build();

        generator.generateTables( params );
    }
}
