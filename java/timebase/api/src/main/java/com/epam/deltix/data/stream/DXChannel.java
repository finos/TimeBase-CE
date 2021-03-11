package com.epam.deltix.data.stream;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.util.security.AuthorizationController;

public interface DXChannel<T> extends AuthorizationController.ProtectedResource {
    /**
     *  Returns the key, which uniquely identifies the channel
     */
    String                                  getKey();

    /**
     *  Returns a user-readable short name.
     */
    String                                  getName();

    /**
     *  Returns a user-readable multi-line description.
     */
    String                                  getDescription();

    /**
     *  Returns the class descriptors associated with this channel
     */
    RecordClassDescriptor[]                 getTypes();

    /**
     *  <p>Opens a source for reading data from this channel, according to the
     *  specified preferences. Iterator-like approach to consume messages:
     *  <code>
     *      while (source.next())
     *          source.getMessage()
     *  </code>
     *  </p>
     *
     *  @return A message source to read messages from.
     */
    MessageSource<T> createConsumer(ChannelPreferences options);

    /**
     *  Creates a channel for loading data. The publisher must be closed
     *  when the loading process is finished.
     *
     *  @return A consumer of messages to be loaded into the channel.
     */
    MessageChannel<T> createPublisher(ChannelPreferences options);
}
