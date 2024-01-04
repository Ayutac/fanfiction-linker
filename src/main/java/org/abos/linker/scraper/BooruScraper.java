package org.abos.linker.scraper;

import org.abos.common.LogUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public final class BooruScraper {

    private static final String BASE_URL = "https://twifanworks.shelter.moe";

    private static final String IMAGE_PAGE = "/post/view/";

    public static final String SHM_SESSION_NAME = "shm_session";

    public static final int TIME_OUT = 100; // in milliseconds

    private static final Logger LOGGER = LogManager.getLogger(WikiScraper.class);

    public BooruScraper() {
        /* Nothing to initialize. */
    }

    public Map<Integer, ZonedDateTime> scrapeUploadTimes(final int maxIndex, final String sessionCookie) throws IOException, InterruptedException {
        LOGGER.info("Scraping upload times from Booru...");
        final Instant start = Instant.now();
        // scrape all upload times
        final Map<Integer, ZonedDateTime> result = new HashMap<>();
        for (int index = 1; index <= maxIndex; index++) {
            Connection connection = Jsoup.connect(BASE_URL + IMAGE_PAGE + index);
            if (sessionCookie != null) {
                connection = connection
                        .cookie(SHM_SESSION_NAME, sessionCookie)
                        .cookie("shm_user", "Ayutac");
            }
            final Document doc;
            try {
                doc = connection.get();
            }
            catch (HttpStatusException ex) {
                LOGGER.warn("Page with index {} is missing!", index);
                continue;
            }
            final Elements content = doc.getElementsByTag("time");
            final Element time = content.get(0);
            result.put(index, ZonedDateTime.parse(time.attr("datetime"), DateTimeFormatter.ISO_DATE_TIME));
            Thread.sleep(TIME_OUT);
        }
        final Duration time = Duration.between(start, Instant.now());
        LOGGER.info(LogUtil.LOG_TIME_MSG, "Scraping upload times from Booru", time.toMinutes(), time.toSecondsPart());
        return result;
    }
}
