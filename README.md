## DHIS 2 Ad-hoc Tool

The ad-hoc tool is written in Java. It provides access to the DHIS 2 Java APIs and services.

### Overview

The purpose of this tool is to assist in performing ad-hoc tasks which benefits from having the DHIS 2 service layer accessible. Examples of such tasks are writing complex custom data entry forms to file and performing database operations which cannot easily be solved with SQL.

The ad-hoc tool is based on the command pattern, and lets you write your own command classes which perform the actual work.

### Getting started

The `RunMe.java` class is the starting point, and this class can be run as a Java project using your IDE or the command line.

See the `org.hisp.dhis.adhoc.command` package for sample command classes.

Command classes must be annotated with the `@Executed` annotation, mapped as a Spring bean if necessary and added to the commands method in `RunMe.java`.

**Step 1**: Create a command class. Put this class in the `org.hisp.dhis.adhoc.command` package. An example command can look like this:

```
public class GetSystemInfoCommand
{
    @Autowired
    private SystemService systemService;

    @Executed
    @Transactional
    public void execute()
    {    
        SystemInfo systemInfo = systemService.getSystemInfo();
        
        System.out.println( String.format( "System info: [%s]", systemInfo ) );
    }
}
```

**Step 2**: Add the command as a Spring bean in `beans.xml`. As an example:

```
<bean id="getSystemInfoCommand" class="org.hisp.dhis.adhoc.command.GetSystemInfoCommand" /> 
```

**Step 3**: In class `RunMe.java`, include the bean identifier of your command bean in the `COMMANDS` list. As an example:

```
ImmutableList<String> COMMANDS = ImmutableList.of( "getSystemInfoCommand" );
```

The DHIS 2 version to target can be set in the `pom.xml` file.
