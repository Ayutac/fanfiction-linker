package org.abos.linker.scraper;

import org.abos.linker.LinkerUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public final class TestBooruScraper {

    @Test
    public void testScrapeUploadTimes() throws IOException, InterruptedException {
        final Map<Integer, ZonedDateTime> uploadTimes = new BooruScraper().scrapeUploadTimes(4141, System.getProperty(BooruScraper.SHM_SESSION_NAME));
        LinkerUtil.createCsvFromUploadTimes(uploadTimes, "uploads.csv");
    }

    @Test
    public void testScrapeTagCounts() throws IOException {
        final Map<String, Integer> tags = new BooruScraper().scrapeTagCounts();
        final int[] otherCount = new int[] {0};
        // by character
        final double characterThreshold = 0.01*LinkerUtil.count(tags, "character:");
        final Map<String, Integer> characters = new HashMap<>();
        tags.forEach((tag, count) -> {
            if (tag.startsWith("character:")) {
                if (count > characterThreshold) {
                    characters.put(tag.substring(tag.indexOf(':')+1), count);
                }
                else {
                    otherCount[0] += count;
                }
            }
        });
        characters.put("Others", otherCount[0]);
        System.out.println(LinkerUtil.toCsvString(characters));
        // by artist
        final double artistThreshold = 0.01*LinkerUtil.count(tags, "artist:");
        final Map<String, Integer> artists = new HashMap<>();
        otherCount[0] = 0;
        tags.forEach((tag, count) -> {
            if (tag.startsWith("artist:")) {
                if (count > artistThreshold) {
                    artists.put(tag.substring(tag.indexOf(':')+1), count);
                }
                else {
                    otherCount[0] += count;
                }
            }
        });
        artists.put("Others", otherCount[0]);
        System.out.println(LinkerUtil.toCsvString(artists));
        // by volume
        final Map<String, Integer> volumes = new HashMap<>();
        tags.forEach((tag, count) -> {
            if (tag.startsWith("spoiler:volume") || tag.equals("spoiler:book1") || tag.equals("spoiler:book2")) {
                volumes.put(tag.substring(tag.indexOf(':')+1), count);
            }
        });
        System.out.println(LinkerUtil.toCsvString(volumes));
        // by volume
        final Map<String, Integer> books = new HashMap<>();
        tags.forEach((tag, count) -> {
            if (tag.startsWith("spoiler:book")) {
                books.put(tag.substring(tag.indexOf(':')+1), count);
            }
        });
        System.out.println(LinkerUtil.toCsvString(books));
    }

}
