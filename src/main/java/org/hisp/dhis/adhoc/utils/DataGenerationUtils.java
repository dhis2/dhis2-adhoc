package org.hisp.dhis.adhoc.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import org.hisp.dhis.option.OptionSet;
import org.springframework.core.io.ClassPathResource;

import com.csvreader.CsvReader;

public class DataGenerationUtils
{
    private static SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

    private static List<String> womenFirstNames = null;
    private static List<String> menFirstNames = null;
    private static List<String> lastNames = null;

    private static void loadNames()
        throws Exception
    {
        // TODO: Questionable thread safety
        if ( menFirstNames == null )
        {
            CsvReader maleReader = new CsvReader( new ClassPathResource( "male_names.csv" ).getInputStream(), UTF_8 );
            menFirstNames = new ArrayList<String>();
            lastNames = new ArrayList<>();

            while ( maleReader.readRecord() )
            {
                menFirstNames.add( maleReader.get( 0 ) );
                lastNames.add( maleReader.get( 1 ) );
            }
            maleReader.close();

            CsvReader femaleReader = new CsvReader( new ClassPathResource( "female_names.csv" ).getInputStream(), UTF_8 );
            womenFirstNames = new ArrayList<String>();
            while ( femaleReader.readRecord() )
            {
                womenFirstNames.add( femaleReader.get( 0 ) );
                lastNames.add( femaleReader.get( 1 ) );
            }
            femaleReader.close();
        }
    }

    public static String getRandomFirstName( boolean isWoman )
        throws Exception
    {
        loadNames();

        return isWoman ? womenFirstNames.get( new Random().nextInt( womenFirstNames.size() ) )
            : menFirstNames.get( new Random().nextInt( menFirstNames.size() ) );
    }

    public static String getRandomLastName()
        throws Exception
    {
        loadNames();

        return lastNames.get( new Random().nextInt( womenFirstNames.size() ) );
    }

    public static String getRandomDateString( int startYear, int endYear )
    {
        GregorianCalendar gc = new GregorianCalendar();
        int year = randBetween( startYear, endYear );
        gc.set( Calendar.YEAR, year );
        int dayOfYear = randBetween( 1, gc.getActualMaximum( Calendar.DAY_OF_YEAR ) );
        gc.set( Calendar.DAY_OF_YEAR, dayOfYear );

        return dateFormat.format( gc.getTime() );
    }

    public static int randBetween( int start, int end )
    {
        return start + (int) Math.round( Math.random() * (end - start) );
    }

    public static String getRandomOptionSetCode( OptionSet optionSet )
    {
        int no = optionSet.getOptions().size();
        int r = new Random().nextInt( no );
        return optionSet.getOptions().get( r ).getCode();
    }

    public static String getRandomBoolString()
    {
        return String.valueOf( new Random().nextBoolean() );
    }
}
