package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;

import java.time.Duration;

public final class DefaultSamplingProvider implements SamplingProvider {
    @Override
    public CreateMessageResponse createMessage(CreateMessageRequest request, Duration timeoutMillis) throws InterruptedException {
        var shouldReject = request.messages().stream()
                .map(SamplingMessage::content)
                .filter(ContentBlock.Text.class::isInstance)
                .map(ContentBlock.Text.class::cast)
                .map(ContentBlock.Text::text)
                .anyMatch("reject"::equalsIgnoreCase);
        if (shouldReject) {
            throw new InterruptedException("User rejected sampling request");
        }
        return new CreateMessageResponse(
                Role.ASSISTANT,
                new ContentBlock.Text("ok", null, null),
                "mock-model",
                "endTurn",
                null);
    }

}
