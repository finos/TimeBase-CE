package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.timebase.messages.IdentityKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Fluent-style API for setting up topic loader channel preferences.
 *
 * @author Alexei Osipov
 */
public class PublisherPreferences extends TopicChannelPreferences<PublisherPreferences> {
    private List<? extends IdentityKey> initialEntitySet;

    public PublisherPreferences() {
    }

    /**
     * @param initialEntitySet initial entry set (may be empty) - list of known {@link IdentityKey} to be used
     */
    public PublisherPreferences setInitialEntitySet(@Nonnull List<? extends IdentityKey> initialEntitySet) {
        this.initialEntitySet = initialEntitySet;
        return this;
    }

    @Nullable
    public List<? extends IdentityKey> getInitialEntitySet() {
        return initialEntitySet;
    }

    public static PublisherPreferences from(ChannelPreferences channelPreferences) {
        if (channelPreferences instanceof PublisherPreferences) {
            return (PublisherPreferences) channelPreferences;
        }
        return new PublisherPreferences().copyFrom(channelPreferences);
    }
}
