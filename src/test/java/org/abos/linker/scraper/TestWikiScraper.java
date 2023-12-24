package org.abos.linker.scraper;

import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Test class for {@link WikiScraper}.
 */
public class TestWikiScraper {

    @Test
    public void testScrapeCharacterTags() throws IOException {
        ScraperTestUtil.doTestBlockingQueue(WikiScraper.TIME_OUT, new WikiScraper().scrapeCharacterTags());
    }
}
