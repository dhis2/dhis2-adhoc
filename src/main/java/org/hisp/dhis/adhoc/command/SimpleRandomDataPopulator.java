package org.hisp.dhis.adhoc.command;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.system.util.Clock;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Command for populating random data values.
 *
 * The popular starts with data sets, and discovers org units, data elements
 * and category option combinations to populate.
 *
 * @author Lars Helge Overland
 */
public class SimpleRandomDataPopulator
{
    private static final Log log = LogFactory.getLog( SimpleRandomDataPopulator.class );

    // -------------------------------------------------------------------------
    // Configuration: Change these variables according to your environment
    // -------------------------------------------------------------------------

    private static final String DATA_SET_UID = "SyaF6Igh6ax";
    private static final Date START_DATE = new DateTime( 2014, 1, 1, 0, 0 ).toDate();
    private static final Date END_DATE = new DateTime( 2020, 1, 1, 0, 0 ).toDate();

    // -------------------------------------------------------------------------
    // Populator
    // -------------------------------------------------------------------------

    private final IdentifiableObjectManager idObjectManager;

    private final PeriodService periodService;

    private final BatchHandlerFactory batchHandlerFactory;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public SimpleRandomDataPopulator( IdentifiableObjectManager idObjectManager, PeriodService periodService,
        BatchHandlerFactory batchHandlerFactory, TransactionTemplate transactionTemplate )
    {
        this.idObjectManager = idObjectManager;
        this.periodService = periodService;
        this.batchHandlerFactory = batchHandlerFactory;
        this.transactionTemplate = transactionTemplate;
    }

    @Executed
    public void execute()
    {
        // Periods populated in individual transaction to be present in database for data population
        List<Period> periods = transactionTemplate.execute( status -> getPeriods() );
        long count = transactionTemplate.execute( status -> populateData( periods ) );
        log.info( String.format( "Done, data values populated: %d", count ) );
    }

    private long populateData( List<Period> pes )
    {
        Clock clock = new Clock( log ).startClock();
        long dvCount = 0;
        Date dt = new Date();
        Random r = new Random();

        final DataSet ds = idObjectManager.get( DataSet.class, DATA_SET_UID );
        final Set<DataElement> des = ds.getDataElements();
        final Set<OrganisationUnit> ous = ds.getSources();

        final Map<String, Double> UID_WEIGHT_MAP = new HashMap<>();
        des.forEach( de -> UID_WEIGHT_MAP.put( de.getUid(), r.nextDouble() ) );
        ous.forEach( ou -> UID_WEIGHT_MAP.put( ou.getUid(), r.nextDouble() ) );
        pes.forEach( pe -> UID_WEIGHT_MAP.put( pe.getIsoDate(), r.nextDouble() ) );

        BatchHandler<DataValue> handler = batchHandlerFactory.createBatchHandler( DataValueBatchHandler.class ).init();

        log.info( String.format( "Generating random data for data set: '%s'", ds.getName() ) );
        log.info( String.format( "Data elements: %d, org units: %d, periods: %d", des.size(), ous.size(), pes.size() ) );

        for ( Period pe : pes )
        {
            for ( OrganisationUnit ou : ous )
            {
                for ( DataElement de : des )
                {
                    for ( CategoryOptionCombo coc : de.getCategoryOptionCombos() )
                    {
                        for ( CategoryOptionCombo aoc : ds.getCategoryCombo().getOptionCombos() )
                        {
                            DataValue dv = new DataValue( de, pe, ou, coc, aoc, null, "", dt, "" );

                            if ( de.getValueType().isBoolean() )
                            {
                                dv.setValue( String.valueOf( r.nextBoolean() ) );
                            }
                            else
                            {
                                Double deWgt = UID_WEIGHT_MAP.get( de.getUid() );
                                Double peWgt = UID_WEIGHT_MAP.get( pe.getIsoDate() );
                                Double ouWgt = UID_WEIGHT_MAP.get( ou.getUid() );
                                Double weight = deWgt + peWgt + ouWgt;
                                Double randVal = r.nextDouble();
                                Double value = randVal * weight * 10;

                                dv.setValue( String.valueOf( Math.round( value ) ) );

                                log.trace( String.format( "Value: %.3f, weight: %.3f, rand val: %.3f",
                                    value, weight, randVal ) );
                            }

                            handler.addObject( dv );
                            dvCount++;

                            log.trace( String.format( "Added data value: %s for de: %s, pe: %s, ou: %s, coc: %s, aoc: %s",
                                dv.getValue(), de.getUid(), pe.getIsoDate(), ou.getUid(), coc.getUid(), aoc.getUid() ) );
                        }
                    }
                }
            }
        }

        handler.flush();

        clock.logTime( String.format( "Data population completed, values added: %d", dvCount ) );

        return dvCount;
    }

    private List<Period> getPeriods()
    {
        final DataSet ds = idObjectManager.get( DataSet.class, DATA_SET_UID );
        CalendarPeriodType pt = (CalendarPeriodType) ds.getPeriodType();
        List<Period> pes = pt.generatePeriods( START_DATE, END_DATE );
        return periodService.reloadPeriods( pes );
    }
}
