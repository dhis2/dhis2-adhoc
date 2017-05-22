package org.hisp.dhis.adhoc.command;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.commons.collection.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

public class RandomDataPopulator
{
    private static final Log log = LogFactory.getLog( RandomDataPopulator.class );
    
    private static final String DS = "Lpw6GcnTrmS";
    private static final String DE_WEIGHT = "h0xKKjijTdI";
    private static final int OU_LEVEL = 4;
    private static final String PE_WEIGHT = "2016";
    private static final List<String> PERIODS = Arrays.asList( 
        "201601", "201602", "201603", "201604", "201605", "201606", "201607", "201608", "201609", "201610","201611", "201612",
        "201701", "201702", "201703", "201704", "201705", "201706", "201707", "201708", "201709", "201710","201711", "201712" 
    );
    
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
    
    @Transactional
    @Executed
    public void execute()
        throws Exception
    {
        // ---------------------------------------------------------------------
        // Organisation units
        // ---------------------------------------------------------------------

        List<OrganisationUnit> ous = new ArrayList<OrganisationUnit>( organisationUnitService.getOrganisationUnitsAtLevel( OU_LEVEL ) );
        Collections.shuffle( ous );
        ous = ListUtils.subList( ous, 0, 800 );
        
        log.info( "Organisation units: " + ous.size() );

        // ---------------------------------------------------------------------
        // Periods (might fail if not present in database due to single tx)
        // ---------------------------------------------------------------------

        List<Period> pes = periodService.reloadIsoPeriods( PERIODS );
        
        log.info( "Periods: " + pes );

        // ---------------------------------------------------------------------
        // Data set
        // ---------------------------------------------------------------------
        
        DataSet dataSet = dataSetService.getDataSet( DS );
        
        log.info( "Data set: '" + dataSet.getName() + "', data elements: " + dataSet.getDataElements().size() );

        // ---------------------------------------------------------------------
        // Category option combinations
        // ---------------------------------------------------------------------
        
        DataElementCategoryOptionCombo defaultAoc = categoryService.getDefaultDataElementCategoryOptionCombo();

        Set<DataElementCategoryOptionCombo> aocs = dataSet != null ? dataSet.getCategoryCombo().getOptionCombos() : Sets.newHashSet( defaultAoc );
        
        log.info( "Attribute option combos: " + aocs );
        
        // ---------------------------------------------------------------------
        // Weight data
        // ---------------------------------------------------------------------
        
        DataElement deWeight = dataElementService.getDataElement( DE_WEIGHT );
        Period peWeight = periodService.reloadIsoPeriod( PE_WEIGHT );
        
        Collection<DataValue> values = dataValueService.getDataValues( Sets.newHashSet( deWeight ), Sets.newHashSet( peWeight ), ous );
        
        Map<String, String> orgUnitValueMap = values.stream().collect( Collectors.toMap( v -> v.getSource().getUid(), v -> v.getValue() ) );
                
        log.info( "Weight data values: " + orgUnitValueMap.keySet().size() );
        
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
            log.info( "Generating data for period: " + pe );
            
            double peFactor = ( ( r.nextInt( 60 ) + 70 ) / 100d );
            
            for ( OrganisationUnit ou : ous )
            {                
                String val = orgUnitValueMap.get( ou.getUid() );
            
                for ( DataElement de : dataSet.getDataElements() )
                {
                    double deFactor = ( ( r.nextInt( 60 ) + 70 ) / 100d );
                    
                    for ( DataElementCategoryOptionCombo coc : de.getCategoryOptionCombos() )
                    {
                        for ( DataElementCategoryOptionCombo aoc : aocs )
                        {
                            if ( val != null )
                            {
                                DataValue dv = new DataValue( de, pe, ou, coc, aoc, String.valueOf( getVal( val, peFactor, deFactor ) ), "", d, "" );
                                handler.addObject( dv );
                                dvCount++;
                            }
                        }
                    }
                }
            }
            
            log.info( "Done for " + ( ++peCount ) + " out of " + peTotal );
        }
        
        handler.flush();
        
        log.info( "Data population completed, added " + dvCount + " data values" );
    }
    
    private Integer getVal( String value, double peFactor, double deFactor )
    {
        Random r = new Random();
        Double val = Double.parseDouble( value );
        Double delta = 80 * ( r.nextDouble() - 0.5 );
        Double weightedVal = ( val * 0.2 ) + delta;
        weightedVal = weightedVal * peFactor * deFactor;
        return weightedVal.intValue();
    }    
}
