package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class CreateTopicRequest {
    private final String topicKey;
    private final List<RecordClassDescriptor> types;
    private final Collection<? extends IdentityKey> initialEntitySet;
    private final String targetStream;

    public CreateTopicRequest(String topicKey, List<RecordClassDescriptor> types, @Nullable Collection<? extends IdentityKey> initialEntitySet, @Nullable String targetStream) {
        this.topicKey = topicKey;
        this.types = types;
        if (initialEntitySet == null) {
            initialEntitySet = Collections.emptyList();
        }
        this.initialEntitySet = initialEntitySet;
        this.targetStream = targetStream;
    }

    @Nonnull
    public String getTopicKey() {
        return topicKey;
    }

    @Nonnull
    public List<RecordClassDescriptor> getTypes() {
        return types;
    }

    @Nonnull
    public Collection<? extends IdentityKey> getInitialEntitySet() {
        return initialEntitySet;
    }

    @Nullable
    public String getTargetStream() {
        return targetStream;
    }
}
