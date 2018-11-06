package org.hisp.dhis.adhoc.command;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.adhoc.annotation.Executed;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class SystemSettingChecker
{
    private Log log = LogFactory.getLog( SystemSettingChecker.class );

    @Autowired
    private SystemSettingManager manager;

    @Executed
    @Transactional
    public void execute()
    {
        for ( SettingKey key : SettingKey.values() )
        {
            log.info( "Checking key: " + key );

            Serializable value = manager.getSystemSetting( key );

            log.info( value );
        }

        manager.getAllSystemSettings();
    }
}
