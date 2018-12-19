package org.hisp.dhis.adhoc.command;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class PeriodPopulator
{
    private static final CalendarPeriodType PERIOD_TYPE = new WeeklyPeriodType();
    private static final Date START_DATE = new DateTime( 2016, 1, 1, 0, 0 ).toDate(); // Start date for periods to populate
    private static final Date END_DATE = new DateTime( 2017, 12, 31, 0, 0 ).toDate(); // End date for periods to populate

    private static final Log log = LogFactory.getLog( PeriodPopulator.class );

    @Autowired
    private PeriodService periodService;

    @Transactional
    @Executed
    public void execute()
        throws Exception
    {
        List<Period> pes = PERIOD_TYPE.generatePeriods( START_DATE, END_DATE );
        pes = periodService.reloadPeriods( pes );

        log.info( String.format( "Populated periods: %d", pes.size() ) );
    }
}
