package com.amannmalik.mcp.client.sampling;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Simple in-memory provider that waits for a queued response. */
public final class BlockingSamplingProvider implements SamplingProvider {
    private final BlockingQueue<CreateMessageResponse> responses = new LinkedBlockingQueue<>();

    /** Queue a response to the next {@link #createMessage(CreateMessageRequest)} call. */
    public void respond(CreateMessageResponse response) {
        if (response == null) throw new IllegalArgumentException("response is required");
        responses.offer(response);
    }

    @Override
    public CreateMessageResponse createMessage(CreateMessageRequest request) throws IOException {
        try {
            return responses.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
