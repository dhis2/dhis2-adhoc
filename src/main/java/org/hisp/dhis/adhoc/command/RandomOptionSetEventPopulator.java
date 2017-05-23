package org.hisp.dhis.adhoc.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.util.Timer;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Event populator for event programs which requires that all data elements
 * have option sets.
 */
public class RandomOptionSetEventPopulator
{
    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private static final String PR = "bMcwwoVnbSR"; // Program to populate
    private static final int OU_LEVEL = 4; // Level of org units to populate
    private static final int START_YEAR = 2016; // Start year for two-year period to populate
    private static final int EVENT_NO = 20; // Number of events to create

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private static final Log log = LogFactory.getLog( RandomOptionSetEventPopulator.class );
        
    @Autowired
    private EventService eventService;
    
    @Autowired
    private ProgramService programService;
    
    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Populator
    // -------------------------------------------------------------------------

    @Executed
    @Transactional
    public void execute()
    {
        Program program = programService.getProgram( PR );
        ProgramStage stage = program.getProgramStages().iterator().next();
        List<DataElement> dataElements = stage.getAllDataElements();
        List<DataElementCategoryOptionCombo> aocs = program.getCategoryCombo().getSortedOptionCombos();

        log.info( String.format( "Program: %s, '%s', stage: %s", program.getUid(), program.getName(), stage.getUid() ) );
        log.info( String.format( "Category combo: %s", program.getCategoryCombo().getName() ) );
        
        List<OrganisationUnit> ous = new ArrayList<OrganisationUnit>( organisationUnitService.getOrganisationUnitsAtLevel( OU_LEVEL ) );
        
        List<Event> events = new ArrayList<Event>();
        
        Assert.notNull( program, String.format( "Program must be specified: %s", PR ) );
        Assert.isTrue( !dataElements.isEmpty(), "At least one data element must be part of program" );
        
        for ( int i = 0; i < EVENT_NO; i++ )
        {
            DateTime date = new DateTime( START_YEAR, 1, 1, 12, 5 ).plusDays( new Random().nextInt( 726 ) );
            
            Event event = new Event();
            event.setStatus( EventStatus.COMPLETED );
            event.setProgram( program.getUid() );
            event.setProgramStage( stage.getUid() );
            event.setOrgUnit( ous.get( new Random().nextInt( ous.size() ) ).getUid() );
            event.setEventDate( DateUtils.getLongDateString( date.toDate() ) );
            event.setAttributeOptionCombo( aocs.get( new Random().nextInt( aocs.size() ) ).getUid() );
            
            for ( DataElement de : dataElements )
            {
                Assert.isTrue( de.hasOptionSet(), String.format( "Data element must have option set: %s", de.getUid() ) );
                
                DataValue dv = new DataValue( de.getUid(), getRandomOption( de.getOptionSet() ) );
                
                event.getDataValues().add( dv );
            }
            
            events.add( event );
        }
        
        log.info( "Populating events: " + events.size() );

        Timer t = new Timer().start();
        
        ImportSummaries summaries = eventService.addEvents( events, new ImportOptions() );
        
        long s = t.getTimeInS();
        double a = EVENT_NO / Math.max( s, 1 );
        
        log.info( summaries );
        
        log.info( "Event population done, seconds: " + s + ", event/s: " + a );
    }
    
    private String getRandomOption( OptionSet optionSet )
    {
        int size = optionSet.getOptions().size();
        int index = new Random().nextInt( size );
        return optionSet.getOptions().get( index ).getCode();
    }
}
