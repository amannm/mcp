package com.amannmalik.mcp.client.elicitation;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public final class BlockingElicitationProvider implements ElicitationProvider {
    private final BlockingQueue<ElicitationResponse> responses = new LinkedBlockingQueue<>();


    public void respond(ElicitationResponse response) {
        if (response == null) throw new IllegalArgumentException("response is required");
        responses.offer(response);
    }

    @Override
    public ElicitationResponse elicit(ElicitationRequest request, long timeoutMillis) throws InterruptedException {
        ElicitationResponse resp = timeoutMillis <= 0
                ? responses.take()
                : responses.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        return resp != null ? resp : new ElicitationResponse(ElicitationAction.CANCEL, null);
    }
}
