package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  A failure to parse configuration, carrying the report of that failure with it.
 *
 *  <p><b>Whoever throws one of these owes it a complete account of what went wrong.</b> That is
 *  the whole point of the class, and it is an obligation rather than a convenience, because
 *  nobody logs anything on its behalf: a caller that catches a {@code ConfigParseException}
 *  drains {@link #getDelayedLogItems} and does no more. Throw one carrying an empty list and the
 *  failure passes in total silence.</p>
 *
 *  <p>The reason the items have to travel on the exception is that there is no other way out. A
 *  {@link PropertiesConfigSource} reports through the {@link PropertiesConfigSource.Parse} it
 *  returns, and a Parse is only ever constructed on the success path &mdash; so everything a
 *  source accumulated before it threw is discarded at the throw. Sources cannot simply log
 *  instead: they run while MLog is still reading its own configuration, which is why
 *  {@link DelayedLogItem} exists at all.</p>
 *
 *  <p>So the rule for an implementor is: before throwing, put into the list every item you would
 *  have returned on the Parse, plus an item describing the failure itself. Pass the list you have
 *  been accumulating into; it is copied, not retained.</p>
 *
 *  @see OwnLogCarryingMissingFileException
 */
public class ConfigParseException extends Exception
{
    private final List<DelayedLogItem> delayedLogItems;

    public ConfigParseException(String msg, Throwable cause, List<DelayedLogItem> delayedLogItems)
    {
        super( msg, cause );
        this.delayedLogItems =
            Collections.unmodifiableList( new ArrayList<DelayedLogItem>( delayedLogItems == null
                                                                            ? Collections.<DelayedLogItem>emptyList()
                                                                            : delayedLogItems ) );
    }

    /**
     *  The report this exception carries. Never null.
     *
     *  <p>A caller that swallows this exception <b>must</b> drain this list, or the account the
     *  thrower prepared is never made and the failure goes unmentioned.</p>
     */
    public List<DelayedLogItem> getDelayedLogItems()
    { return delayedLogItems; }
}
