package com.amannmalik.mcp.client.sampling;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class BlockingSamplingProvider implements SamplingProvider {
    private final BlockingQueue<CreateMessageResponse> responses = new LinkedBlockingQueue<>();

    public void respond(CreateMessageResponse response) {
        if (response == null) throw new IllegalArgumentException("response is required");
        responses.offer(response);
    }

    @Override
    public CreateMessageResponse createMessage(CreateMessageRequest request, long timeoutMillis) throws InterruptedException {
        return timeoutMillis <= 0
                ? responses.take()
                : responses.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
