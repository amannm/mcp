package com.amannmalik.mcp.client.roots;

import java.io.IOException;
import java.util.List;


public interface RootsProvider extends AutoCloseable {
    
    List<Root> list() throws IOException;

    
    RootsSubscription subscribe(RootsListener listener) throws IOException;

    @Override
    default void close() throws IOException {}
}
