package org.abos.linker.scraper;

import org.abos.linker.LinkerUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;

public final class TestBooruScraper {

    @Test
    public void testScrapeUploadTimes() throws IOException, InterruptedException {
        final Map<Integer, ZonedDateTime> uploadTimes = new BooruScraper().scrapeUploadTimes(4141, System.getProperty(BooruScraper.SHM_SESSION_NAME));
        LinkerUtil.createCsvFromUploadTimes(uploadTimes, "uploads.csv");
    }

}
