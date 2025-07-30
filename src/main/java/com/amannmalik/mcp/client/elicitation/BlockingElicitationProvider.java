package com.amannmalik.mcp.client.elicitation;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class BlockingElicitationProvider implements ElicitationProvider {
    private final BlockingQueue<ElicitResult> responses = new LinkedBlockingQueue<>();

    public void respond(ElicitResult response) {
        if (response == null) throw new IllegalArgumentException("response is required");
        responses.offer(response);
    }

    @Override
    public ElicitResult elicit(ElicitRequest request, long timeoutMillis) throws InterruptedException {
        ElicitResult resp = timeoutMillis <= 0
                ? responses.take()
                : responses.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        return resp != null ? resp : new ElicitResult(ElicitationAction.CANCEL, null, null);
    }
}
