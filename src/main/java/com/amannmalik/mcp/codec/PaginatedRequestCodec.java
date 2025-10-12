package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Request;

public final class PaginatedRequestCodec {
    public static final JsonCodec<Request.PaginatedRequest> INSTANCE =
            AbstractEntityCodec.paginatedRequest(
                    Request.PaginatedRequest::cursor,
                    Request.PaginatedRequest::_meta,
                    Request.PaginatedRequest::new);

    private PaginatedRequestCodec() {
    }
}
