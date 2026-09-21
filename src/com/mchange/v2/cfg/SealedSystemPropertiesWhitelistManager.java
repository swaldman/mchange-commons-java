package com.mchange.v2.cfg;

import java.util.*;
import com.mchange.v2.log.*;

public class SealedSystemPropertiesWhitelistManager extends WhitelistManager
{
    public SealedSystemPropertiesWhitelistManager(String baseKey, String deprecatedKey)
    { super( baseKey, deprecatedKey ); }

    @Override
    public WhitelistInfo collectWhitelistInfoSyspropsPropertiesConfig(PropertiesConfig pcfg, MLogger logger)
    { return collectWhitelistInfoSyspropsPropertiesConfig(SealedSystemProperties.get(), pcfg, logger); }
}
