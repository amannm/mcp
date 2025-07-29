package com.amannmalik.mcp.client.roots;

import java.util.List;


public interface RootsProvider extends AutoCloseable {

    List<Root> list();


    RootsSubscription subscribe(RootsListener listener);

    @Override
    default void close() {}
}
