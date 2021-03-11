package com.epam.deltix.qsrv.hf.topic.consumer;

import io.aeron.Image;
import io.aeron.UnavailableImageHandler;

/**
 * Allows to compose two {@link UnavailableImageHandler} into one.
 *
 * @author Alexei Osipov
 */
class DoubleUnavailableImageHandler implements UnavailableImageHandler {
    private final UnavailableImageHandler first;
    private final UnavailableImageHandler second;

    DoubleUnavailableImageHandler(UnavailableImageHandler first, UnavailableImageHandler second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void onUnavailableImage(Image image) {
        first.onUnavailableImage(image);
        second.onUnavailableImage(image);
    }
}
