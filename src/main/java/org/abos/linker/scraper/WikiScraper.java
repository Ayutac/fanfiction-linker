package org.abos.linker.scraper;

import org.abos.common.LogUtil;
import org.abos.linker.core.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class WikiScraper {

    private static final String BASE_URL = "https://twiki.shelter.moe";

    private static final String CHARACTER_PAGE = "/Category:Characters";

    public static final int TIME_OUT = 100; // in milliseconds

    private static final Logger LOGGER = LogManager.getLogger(WikiScraper.class);

    public WikiScraper() {
        /* Nothing to initialize. */
    }

    public String scrapeFirstSentence(final String url) throws IOException {
        final Document doc = Jsoup.connect(url).get();
        final Element contentHolder = doc.getElementById("citizen-section-collapsible-0");
        if (contentHolder == null) {
            LOGGER.warn("Missing content holder for {} detected!", url);
            return "";
        }
        final Elements content = contentHolder.getElementsByTag("p");
        if (content.size() == 0) {
            LOGGER.warn("Missing content for {} detected!", url);
            return "";
        }
        final String s = content.get(content.size()-1).text();
        return s.substring(0, s.indexOf('.') + 1);
    }

    /**
     * Scrapes all character off the wiki.
     * @return A synchronized queue of all the characters in the wiki.
     * Note that the last value of the queue will be {@link Tag#DUMMY} to signify its end.
     * @throws IOException If an I/O error occurs.
     */
    public BlockingQueue<Tag> scrapeCharacterTags() throws IOException {
        LOGGER.info("Scraping character tags...");
        final Instant start = Instant.now();
        // scrape all names + links
        final Map<String, String> links = new HashMap<>();
        Document doc = Jsoup.connect(BASE_URL + CHARACTER_PAGE).get();
        boolean firstPage = true;
        while (true) {
            final Element content = doc.getElementById("mw-pages");
            final Element linkNext;
            if (firstPage) {
                linkNext = content.getElementsByTag("a").get(0);
                firstPage = false;
            }
            else {
                linkNext = content.getElementsByTag("a").get(1);
            }
            for (Element group : content.getElementsByClass("mw-category-group")) {
                for (Element entry : group.getElementsByTag("a")) {
                    if (entry.text().equals("Infobox character testing")) {
                        continue;
                    }
                    links.put(entry.text(), BASE_URL + entry.attr("href"));
                }
            }
            if (!linkNext.text().contains("next page")) {
                break;
            }
            try {
                Thread.sleep(TIME_OUT);
            } catch (InterruptedException ex) {
                /* Ignore */
            }
            doc = Jsoup.connect(BASE_URL + linkNext.attr("href")).get();
        }
        // scrape descriptions
        final BlockingQueue<Tag> result = new LinkedBlockingQueue<>();
        new Thread(() -> {
            for (Map.Entry<String, String> entry : links.entrySet()) {
                try {
                    result.add(new Tag(entry.getKey(), scrapeFirstSentence(entry.getValue()), true, false, null, entry.getValue()));
                    Thread.sleep(TIME_OUT);
                } catch (IOException | InterruptedException ex) {
                    /* Ignore */
                }
            }
            result.add(Tag.DUMMY);
            final Duration time = Duration.between(start, Instant.now());
            LOGGER.info(LogUtil.LOG_TIME_MSG, "Scraping character tags", time.toMinutes(), time.toSecondsPart());
        }).start();
        return result;
    }

}