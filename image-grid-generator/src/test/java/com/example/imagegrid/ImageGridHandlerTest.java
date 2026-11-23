package com.example.imagegrid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ImageGridHandlerTest {

    @Test
    void testHandlerInstance() {
        ImageGridHandler handler = new ImageGridHandler();
        assertNotNull(handler);
    }
}
