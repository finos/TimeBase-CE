package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.timebase.messages.IdentityKey;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class AddTopicPublisherRequest {
    private final String topicKey;
    private final List<? extends IdentityKey> initialEntitySet;

    public AddTopicPublisherRequest(String topicKey, @Nullable List<? extends IdentityKey> initialEntitySet) {
        this.topicKey = topicKey;
        if (initialEntitySet == null) {
            initialEntitySet = Collections.emptyList();
        }
        this.initialEntitySet = initialEntitySet;
    }

    public String getTopicKey() {
        return topicKey;
    }

    public List<? extends IdentityKey> getInitialEntitySet() {
        return initialEntitySet;
    }
}
