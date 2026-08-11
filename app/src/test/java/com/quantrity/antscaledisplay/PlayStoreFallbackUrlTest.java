package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.net.URI;

public class PlayStoreFallbackUrlTest {
    @Test
    public void browserFallbacksUseHttpsPlayStoreUrls() throws Exception {
        assertPlayStoreHttps(MainActivity.ANT_RADIO_PLAY_STORE_URL);
        assertPlayStoreHttps(MainActivity.ANT_USB_PLAY_STORE_URL);
    }

    private static void assertPlayStoreHttps(String url) throws Exception {
        URI uri = new URI(url);
        assertEquals("https", uri.getScheme());
        assertEquals("play.google.com", uri.getHost());
    }
}
