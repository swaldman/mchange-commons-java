package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  Nothing was found at an identifier, reported by a source that has <b>already made its own
 *  complete account</b> of the absence &mdash; which is what "own log carrying" means, and what
 *  separates this from an ordinary {@link java.io.FileNotFoundException}.
 *
 *  <p>Absence is normally reported for a source by whoever catches it, as a FINE "could not be
 *  found. Skipping." item. A source throws this instead when that generic item would be wrong or
 *  insufficient: because the absence deserves a louder level than FINE, or because it is not in
 *  fact being skipped. Catchers must therefore defer entirely to
 *  {@link #getDelayedLogItems} and add nothing of their own, or the same absence gets reported
 *  twice, in two different voices.</p>
 *
 *  <p>Concretely, {@code FileUrlPropertiesConfigSource} throws this for all three states of its
 *  {@code required} option: absent-and-required is a veto rather than a skip, absent under
 *  {@code permissions=useronly} with {@code required} unstated warrants a WARNING rather than a
 *  FINE, and only an explicit {@code required=false} gets the ordinary skip item &mdash; which it
 *  supplies itself, via {@link MConfig#skippingFileNotFoundDelayedItem}.</p>
 */
public class OwnLogCarryingMissingFileException extends ConfigParseException
{
    public OwnLogCarryingMissingFileException(String msg, Throwable cause, List<DelayedLogItem> delayedLogItems)
    { super( msg, cause, delayedLogItems ); }
}
