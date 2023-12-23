package org.abos.linker.scraper;

import org.abos.linker.core.Character;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class WikiScraper {

    private static final String BASE_URL = "https://twiki.shelter.moe/";

    private static final String CHARACTER_PAGE = "Category:Characters";

    private static final int TIME_OUT = 100; // in milliseconds

    public WikiScraper() {
        /* Nothing to initialize. */
    }

    public String scrapeFirstSentence(final String url) throws IOException {
        final Document doc = Jsoup.connect(url).get();
        final Elements content = doc.getElementById("citizen-section-collapsible-0").getElementsByTag("p");
        if (content.size() == 0) {
            return "";
        }
        final String s = content.get(content.size()-1).text();
        return s.substring(0, s.indexOf('.') + 1);
    }

    /**
     * Scrapes all character off the wiki.
     * @return A synchronized queue of all the characters in the wiki.
     * Note that the last value of the queue will be {@link Character#DUMMY} to signify its end.
     * @throws IOException If an I/O error occurs.
     */
    public BlockingQueue<Character> scrapeCharacters() throws IOException {
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
                    links.put(entry.text(), BASE_URL + entry.attr("href").substring(1));
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
            doc = Jsoup.connect(BASE_URL + linkNext.attr("href").substring(1)).get();
        }
        // scrape descriptions
        final BlockingQueue<Character> result = new LinkedBlockingQueue<>();
        new Thread(() -> {
            for (Map.Entry<String, String> entry : links.entrySet()) {
                try {
                    result.add(new Character(entry.getKey(), scrapeFirstSentence(entry.getValue()), null, entry.getValue()));
                    Thread.sleep(TIME_OUT);
                } catch (IOException | InterruptedException ex) {
                    /* Ignore */
                }
            }
            result.add(Character.DUMMY);
        }).start();
        return result;
    }

    public static void main(String[] args) throws IOException {
        Queue<Character> queue = new WikiScraper().scrapeCharacters();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            /* Ignore */
        }
        System.out.println(queue.peek());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            /* Ignore */
        }
        System.out.println(queue.size());
    }

}