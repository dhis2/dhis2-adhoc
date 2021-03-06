package org.hisp.dhis.adhoc.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.adhoc.utils.DataGenerationUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class RandomEnrollmentPopulator
{
    private static final Log log = LogFactory.getLog( RandomChildrenPopulator.class );

    @Autowired
    private TrackedEntityInstanceService teiService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService psiService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Executed
    @Transactional
    public void execute()
        throws Exception
    {
        int numberOfRecords = 2000; //Update number of records
        Program p = programService.getProgram( "WSGAb5XwJ3Y" ); //Update program uid

        List<OrganisationUnit> ous = new ArrayList<OrganisationUnit>( p.getOrganisationUnits() );

        TrackedEntityType te = p.getTrackedEntityType();
        boolean isWoman = te.getName().toLowerCase().contains( "mnch" ) ? true : new Random().nextBoolean();

        int totalProgramStageInstanceCount = 0;

        for ( int recordNumber = 0; recordNumber < numberOfRecords; recordNumber++ )
        {
            int programStageInstanceCount = 0;

            DateTime date = new DateTime( DateTime.now().getYear() - 1, 1, 1, 12, 5 ).plusDays( new Random().nextInt( 363 ) );

            OrganisationUnit ou = ous.get( new Random().nextInt( ous.size() ) );

            TrackedEntityInstance tei = new TrackedEntityInstance();

            tei.setTrackedEntityType( te );
            tei.setOrganisationUnit( ou );

            teiService.addTrackedEntityInstance( tei );

            for ( TrackedEntityAttribute att : p.getTrackedEntityAttributes() )
            {
                String an = att.getName().toLowerCase();

                if ( att.isGenerated() )
                {
                    attributeValueService.addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( att, tei, null ) ); //TODO reserve value
                }
                else if ( an.contains( "first" ) && an.contains( "name" ) )
                {
                    attributeValueService.addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( att, tei, DataGenerationUtils.getRandomFirstName( isWoman ) ) );
                }
                else if ( an.contains( "last" ) && an.contains( "name" ) )
                {
                    attributeValueService.addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( att, tei, DataGenerationUtils.getRandomLastName() ) );
                }
                else if ( ( an.contains( "birth" ) || an.contains( "born" ) ) && att.isDateType() )
                {
                    attributeValueService.addTrackedEntityAttributeValue( new TrackedEntityAttributeValue( att, tei, DataGenerationUtils.getRandomDateString( 1970, 1990 ) ) );
                }
            }

            ProgramInstance pi = programInstanceService.enrollTrackedEntityInstance( tei, p, date.toDate(), date.toDate(), ou );

            for ( ProgramStage ps : p.getProgramStages() )
            {
                int eventsToAdd = ps.getRepeatable() ? DataGenerationUtils.randBetween( 0, 5 ) : DataGenerationUtils.randBetween( 0, 1 );

                for ( int eventCount = 0; eventCount < eventsToAdd; eventCount++ )
                {
                    date = date.plusDays( DataGenerationUtils.randBetween( 1, 50 ) );

                    ProgramStageInstance psi = new ProgramStageInstance( pi, ps );
                    psi.setDueDate( date.toDate() );
                    psi.setExecutionDate( date.toDate() );
                    psi.setOrganisationUnit( ou );

                    programStageInstanceCount++;
                    totalProgramStageInstanceCount++;

                    Set<EventDataValue> dvs = new HashSet<>();

                    for ( ProgramStageDataElement psde : ps.getProgramStageDataElements() )
                    {
                        DataElement de = psde.getDataElement();
                        String dn = de.getName().toLowerCase();

                        if ( de.isNumericType() && dn.contains( "moglobin" ) )
                        {
                            dvs.add( new EventDataValue( de.getUid(), Integer.toString( DataGenerationUtils.randBetween( 6, 25 ) ) ) );
                        }
                        else if ( ValueType.BOOLEAN.equals( de.getValueType() ) )
                        {
                            dvs.add( new EventDataValue( de.getUid(), DataGenerationUtils.getRandomBoolString() ) );
                        }
                    }

                    psi.setEventDataValues( dvs );

                    psiService.addProgramStageInstance( psi );
                }
            }

            log.info( "Created trackedEntityInstance: " + tei.getUid() + " programInstance: " + pi.getUid() +
                " for program: " + p.getName() + " - " + p.getUid() + " with " +
                programStageInstanceCount + " programstageInstances.");
        }

        log.info( "Created " + numberOfRecords + " programInstances with a total of " + totalProgramStageInstanceCount + " programStageInstances");
    }
}