package org.hisp.dhis.adhoc.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.joda.time.DateTime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.commons.collection.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * Populator for generating data values. Change the variables in configuration
 * to your needs.
 * <p>
 * The configuration is based on a data set. Data will be generated for all
 * data elements in the data set and their disaggregation categories and for the
 * attribute categories of the data set.
 */
public class RandomDataPopulator
{
    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private static final String DS = "Nyh6laLdBEJ"; // Data set to populate
    private static final int OU_LEVEL = 4; // Level of org units to populate
    private static final double OU_DENSITY_PERCENTAGE = 0.3d; // Percentage of org units to populate
    private static final Date START_DATE = new DateTime( 2016, 1, 1, 0, 0 ).toDate(); // Start date for periods to populate
    private static final Date END_DATE = new DateTime( 2017, 12, 31, 0, 0 ).toDate(); // End date for periods to populate
    
    private static final boolean USE_RANDOM_VALUES = true; // Whether to use correlated or random values
    private static final String DE_WEIGHT = "h0xKKjijTdI"; // Data element to use as basis for generation
    private static final String PE_WEIGHT = "2016"; // Period to use as basis for generation
    private static final int RANDOM_MIN = 0; // Minimum random value
    private static final int RANDOM_MAX = 10; // Maximum random value
    
    private static final boolean DRY_RUN = false; // Whether to perform a dry run and not persist any values

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private static final Log log = LogFactory.getLog( RandomDataPopulator.class );
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private DataSetService dataSetService;
        
    @Autowired
    private PeriodService periodService;
    
    @Autowired
    private DataElementCategoryService categoryService;
    
    @Autowired
    private BatchHandlerFactory batchHandlerFactory;
    
    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataValueService dataValueService;

    // -------------------------------------------------------------------------
    // Populator
    // -------------------------------------------------------------------------

    @Transactional
    @Executed
    public void execute()
        throws Exception
    {
        Clock clock = new Clock( log ).startClock();
                
        log.info( String.format( "Using random values: %b", USE_RANDOM_VALUES ) );
        
        // ---------------------------------------------------------------------
        // Organisation units
        // ---------------------------------------------------------------------

        List<OrganisationUnit> ous = new ArrayList<OrganisationUnit>( organisationUnitService.getOrganisationUnitsAtLevel( OU_LEVEL ) );
        Collections.shuffle( ous );
        Double maxOus = ous.size() * OU_DENSITY_PERCENTAGE;
        ous = ListUtils.subList( ous, 0, maxOus.intValue() );
        
        log.info( String.format( "Organisation units: %d, max: %3.2f", ous.size(), maxOus ) );

        // ---------------------------------------------------------------------
        // Data set
        // ---------------------------------------------------------------------
        
        DataSet dataSet = dataSetService.getDataSet( DS );
        
        log.info( String.format( "Data set: '%s', period type: %s, data elements: %d", dataSet.getName(), dataSet.getPeriodType().getName(), dataSet.getDataElements().size() ) );

        // ---------------------------------------------------------------------
        // Periods (might fail if not present in database due to single tx)
        // ---------------------------------------------------------------------

        CalendarPeriodType periodType = (CalendarPeriodType) dataSet.getPeriodType();
        List<Period> pes = periodType.generatePeriods( START_DATE, END_DATE );
        pes = periodService.reloadPeriods( pes );
                
        log.info( String.format( "Periods: %d: %s", pes.size(), pes ) );

        // ---------------------------------------------------------------------
        // Category option combinations
        // ---------------------------------------------------------------------
        
        DataElementCategoryOptionCombo defaultAoc = categoryService.getDefaultDataElementCategoryOptionCombo();

        Set<DataElementCategoryOptionCombo> aocs = dataSet != null ? dataSet.getCategoryCombo().getOptionCombos() : Sets.newHashSet( defaultAoc );
        
        log.info( String.format( "Attribute option combos: %d: %s", aocs.size(), aocs ) );
        
        // ---------------------------------------------------------------------
        // Weight data
        // ---------------------------------------------------------------------
        
        DataElement deWeight = dataElementService.getDataElement( DE_WEIGHT );
        Period peWeight = periodService.reloadIsoPeriod( PE_WEIGHT );
        
        Collection<DataValue> values = dataValueService.getDataValues( Sets.newHashSet( deWeight ), Sets.newHashSet( peWeight ), ous );
        
        Map<String, String> orgUnitValueMap = values.stream().collect( Collectors.toMap( v -> v.getSource().getUid(), v -> v.getValue() ) );
        
        // ---------------------------------------------------------------------
        // Setup and generation
        // ---------------------------------------------------------------------
        
        BatchHandler<DataValue> handler = batchHandlerFactory.createBatchHandler( DataValueBatchHandler.class ).init();

        Date d = new Date();
        
        int peTotal = pes.size();
        int peCount = 0;
        long dvCount = 0;

        Random r = new Random();
            
        for ( Period pe : pes )
        {
            log.info( String.format( "Generating data for period: %s, %s", pe.getIsoDate(), pe ) );
            
            double peFactor = ( ( 70 + r.nextInt( 60 ) ) / 100d );
            
            for ( OrganisationUnit ou : ous )
            {                
                String val = orgUnitValueMap.get( ou.getUid() );
            
                for ( DataElement de : dataSet.getDataElements() )
                {
                    double deFactor = ( ( 70 + r.nextInt( 60 ) ) / 100d );
                    
                    for ( DataElementCategoryOptionCombo coc : de.getCategoryOptionCombos() )
                    {
                        for ( DataElementCategoryOptionCombo aoc : aocs )
                        {
                            if ( val != null )
                            {
                                Integer value = USE_RANDOM_VALUES ? 
                                    getRandomVal( peFactor, deFactor ) : 
                                    getCorrelatedVal( val, peFactor, deFactor );
                                    
                                log.trace( String.format( "Value: %d", value ) );
                                
                                if ( !DRY_RUN )
                                {
                                    DataValue dv = new DataValue( de, pe, ou, coc, aoc, String.valueOf( value ), "", d, "" );
                                    handler.addObject( dv );
                                    
                                    log.trace( String.format( "Added value: %s", dv ) );
                                }
                                
                                dvCount++;
                            }
                        }
                    }
                }
            }
            
            peCount++;
            
            log.info( String.format( "Done for %d (%s) out of %d", peCount, pe.getIsoDate(), peTotal ) );
        }
        
        handler.flush();

        clock.logTime( String.format( "Data population completed, values added: %d", dvCount ) );
    }
    
    private Integer getCorrelatedVal( String value, double peFactor, double deFactor )
    {
        Random r = new Random();
        Double val = Double.parseDouble( value );
        Double delta = 80 * ( r.nextDouble() - 0.5 );
        Double weightedVal = ( val * 0.2 ) + delta;
        weightedVal = weightedVal * peFactor * deFactor;
        return weightedVal.intValue();
    }
    
    private Integer getRandomVal( double peFactor, double deFactor )
    {
        Random r = new Random();
        int diff = RANDOM_MAX - RANDOM_MIN;
        Double random = ( r.nextDouble() * diff * peFactor * deFactor ) + RANDOM_MIN;
        random = Math.max( random, RANDOM_MIN );
        random = Math.min( random, RANDOM_MAX );
        return random.intValue();
    }
}
