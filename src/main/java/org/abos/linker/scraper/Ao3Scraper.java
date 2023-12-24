package org.abos.linker.scraper;

import org.abos.linker.core.Author;
import org.abos.linker.core.Fandom;
import org.abos.linker.core.Fanfiction;
import org.abos.linker.core.Tag;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public final class Ao3Scraper {

    private static final String BASE_URL = "https://archiveofourown.org";

    private static final String FANFICTION_PAGE = "/tags/The%20Wandering%20Inn%20-%20pirateaba/works";

    private static final String TWI_FANDOM = "The Wandering Inn - pirateaba";

    private static final int TIME_OUT = 100; // in milliseconds

    public Ao3Scraper() {
        /* Nothing to initialize. */
    }

    public Fanfiction scrapeFanfiction(final Element entry) {
        // scrape heading and authors
        final Elements h4Links = entry.getElementsByTag("h4").get(0).getElementsByTag("a");
        final String title = h4Links.get(0).text();
        final String link = h4Links.get(0).attr("href");
        final List<Author> authors = new LinkedList<>();
        for (int i = 1; i < h4Links.size(); i++) {
            authors.add(new Author(h4Links.get(i).text(), List.of(h4Links.get(i).attr("href"))));
        }
        // scrape fandoms
        final Elements h5Links = entry.getElementsByTag("h5").get(0).getElementsByTag("a");
        final List<Fandom> fandoms = new LinkedList<>();
        for (Element fandom : h5Links) {
            // we ignore the TWI fandom
            if (fandom.text().equals(TWI_FANDOM)) {
                continue;
            }
            // we do not take the fandom link from Ao3
            fandoms.add(new Fandom(fandom.text(), null));
        }
        // scrape required tags (the square ones)
        final Elements requiredTags = entry.getElementsByTag("h5").get(0).nextElementSibling().getElementsByTag("li");
        boolean catFf = false, catFm = false, catMm = false, catGen = false, catMulti = false, catOther = false, completed = false;
        String rating = null;
        for (Element tag : requiredTags) {
            final var type = tag.getElementsByTag("span").get(0).classNames();
            // ignore warning, will be taken from regular tags
            if (type.contains("warnings")) {
                continue;
            }
            if (type.contains("rating")) {
                rating = tag.text();
            }
            else if (type.contains("category")) {
                final String categories = tag.text();
                if (categories.contains("F/F")) {
                    catFf = true;
                }
                if (categories.contains("F/M")) {
                    catFm = true;
                }
                if (categories.contains("M/M")) {
                    catMm = true;
                }
                if (categories.contains("Gen")) {
                    catGen = true;
                }
                if (categories.contains("Multi")) {
                    catMulti = true;
                }
                if (categories.contains("Other")) {
                    catOther = true;
                }
            }
            else if (type.contains("complete-yes")) {
                completed = true;
            }
        }
        if (rating == null) {
            throw new IllegalStateException("Rating was not specified for story " + title + "!");
        }
        // scrape the regular tags
        final Elements tags = entry.getElementsByTag("h6").get(0).nextElementSibling().getElementsByTag("li");
        final List<Tag> tagList = new LinkedList<>();
        boolean warningNoneGiven = false, warningNoneApply = false, warningViolence = false, warningRape = false, warningDeath = false, warningUnderage = false;
        for (Element tag : tags) {
            if (tag.classNames().contains("warnings")) {
                switch (tag.text()) {
                    case "Creator Chose Not To Use Archive Warnings" -> warningNoneGiven = true;
                    case "No Archive Warnings Apply" -> warningNoneApply = true;
                    case "Graphic Depictions Of Violence" -> warningViolence = true;
                    case "Rape/Non-Con" -> warningRape = true;
                    case "Major Character Death" -> warningDeath = true;
                    case "Underage" -> warningUnderage = true;
                    default -> throw new IllegalStateException("Unknown warning " + tag.text() + " encountered!");
                }
            }
            else {
                tagList.add(new Tag(tag.text(), null, tag.classNames().contains("characters"), tag.classNames().contains("relationships"), null, null));
            }
        }
        // scrape stats
        String language = null;
        int chapters = 0, words = 0;
        final Elements stats = entry.getElementsByClass("stats").get(0).getElementsByTag("dd");
        for (Element stat : stats) {
            if (stat.classNames().contains("language")) {
                language = stat.text();
            }
            else if (stat.classNames().contains("chapters")) {
                final String completeChapters = stat.text();
                chapters = Integer.parseInt(completeChapters.substring(0, completeChapters.indexOf('/')).replace(",", ""));
            }
            else if (stat.classNames().contains("words")) {
                words = Integer.parseInt(stat.text().replace(",", ""));
            }
        }
        if (chapters == 0) {
            throw new IllegalStateException("Couldn't find chapters for " + title + "!");
        }
        if (words == 0) {
            throw new IllegalStateException("Couldn't find words for " + title + "!");
        }
        return new Fanfiction(title, chapters, words, language, rating, warningNoneGiven, warningNoneApply, warningViolence, warningRape, warningDeath, warningUnderage, catFf, catFm, catMm, catGen, catMulti, catOther, completed, null, null, link, authors, tagList, fandoms);
    }

    public List<Fanfiction> scrapeFanfictions() throws IOException {
        List<Fanfiction> result = new LinkedList<>();
        Document doc = Jsoup.connect(BASE_URL + FANFICTION_PAGE).get();
        while (true) {
            final Elements group = doc.getElementsByAttributeValue("role", "article");
            final Elements linkNexts = doc.getElementsByClass("next").get(0).getElementsByTag("a");
            final Element linkNext = linkNexts.isEmpty() ? null : linkNexts.get(0);
            for (Element entry : group) {
                result.add(scrapeFanfiction(entry));
            }
            if (linkNext == null) {
                break;
            }
            try {
                Thread.sleep(TIME_OUT);
            } catch (InterruptedException ex) {
                /* Ignore */
            }
            doc = Jsoup.connect(BASE_URL + linkNext.attr("href")).get();
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        List<Fanfiction> list = new Ao3Scraper().scrapeFanfictions();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            /* Ignore */
        }
        System.out.println(list.get(0));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            /* Ignore */
        }
        System.out.println(list.size());
    }

}
