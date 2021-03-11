package com.epam.deltix.qsrv.hf.tickdb.impl;

/**
 *
 */
final class LosslessMessageQueueReader 
    extends MessageQueueReader <LosslessMessageQueue>
{
    private int offset = 0;
    
    LosslessMessageQueueReader (LosslessMessageQueue mq) {
        super (mq);
    }

    @Override
    public void                 close () {
        mfile.rawReaderClosed (this);
    }

    @Override
    public int                  getBufferOffset() {
        return offset;
    }

    @Override
    public int                  getBufferSize() {
        return buffer.length - offset;
    }

    @Override
    protected void              invalidateBuffer() {
        if (available() > 0) {  // buffer contains incomplete message
            offset = buffer.length - bufferPosition;

            // if remaining part > half of buffer - extend buffer
            if (offset > buffer.length / 2) {
                byte[] temp = new byte[buffer.length * 2];
                System.arraycopy(buffer, bufferPosition, temp, 0, offset);
                buffer = temp;
            } else {
                System.arraycopy(buffer, bufferPosition, buffer, 0, offset);
            }

            bufferFileOffset = getCurrentOffset() - offset;
        } else {
            offset = 0;
            bufferFileOffset = getCurrentOffset();
        }

        bufferPosition = 0;
    }
}
