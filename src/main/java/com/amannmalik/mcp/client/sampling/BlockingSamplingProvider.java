package com.amannmalik.mcp.client.sampling;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class BlockingSamplingProvider implements SamplingProvider {
    private final BlockingQueue<CreateMessageResponse> responses = new LinkedBlockingQueue<>();

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
