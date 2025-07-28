package com.amannmalik.mcp.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PaginationTest {
    @Test
    void paginateThroughList() {
        List<Integer> data = IntStream.range(0, 7).boxed().toList();
        Pagination.Page<Integer> p1 = Pagination.page(data, null, 3);
        assertEquals(List.of(0,1,2), p1.items());
        assertNotNull(p1.nextCursor());

        Pagination.Page<Integer> p2 = Pagination.page(data, p1.nextCursor(), 3);
        assertEquals(List.of(3,4,5), p2.items());
        assertNotNull(p2.nextCursor());

        Pagination.Page<Integer> p3 = Pagination.page(data, p2.nextCursor(), 3);
        assertEquals(List.of(6), p3.items());
        assertNull(p3.nextCursor());
    }

    @Test
    void invalidCursor() {
        assertThrows(IllegalArgumentException.class, () -> Pagination.page(List.of(1,2,3), "bad", 2));
    }
}
