package com.synergyhub.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    void builder_ShouldCreatePageResponse() {
        List<String> content = Arrays.asList("Item1", "Item2", "Item3");

        PageResponse<String> response = PageResponse.<String>builder()
                .content(content)
                .pageNumber(0)
                .pageSize(20)
                .totalElements(100)
                .totalPages(5)
                .first(true)
                .last(false)
                .empty(false)
                .build();

        assertEquals(content, response.getContent());
        assertEquals(0, response.getPageNumber());
        assertEquals(20, response.getPageSize());
        assertEquals(100, response.getTotalElements());
        assertEquals(5, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
        assertFalse(response.isEmpty());
    }

    @Test
    void builder_WithEmptyContent_ShouldIndicateEmpty() {
        PageResponse<String> response = PageResponse.<String>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(20)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();

        assertTrue(response.getContent().isEmpty());
        assertTrue(response.isEmpty());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }
}