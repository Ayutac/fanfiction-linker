package org.abos.linker.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class FanfictionBuilder {

    private String title;
    private int chapters;
    private int words;
    private String language;
    private String rating;
    private boolean warningNoneGiven;
    private boolean warningNoneApply;
    private boolean warningViolence;
    private boolean warningRape;
    private boolean warningDeath;
    private boolean warningUnderage;
    private boolean catFf;
    private boolean catFm;
    private boolean catMm;
    private boolean catGen;
    private boolean catMulti;
    private boolean catOther;
    private boolean completed;
    private Instant lastUpdated;
    private Instant lastChecked;
    private String link;
    private List<Author> authors;
    private List<Tag> tags;
    private List<Fandom> crossovers;

    public FanfictionBuilder() {
        /* Default constructor empty on purpose. */
    }

    // TODO JavaDoc necessary fields
    public FanfictionBuilder(final String title, final int chapters, final int words, final Instant lastUpdated, final String link) {
        title(title);
        chapters(chapters);
        words(words);
        lastUpdated(lastUpdated);
        link(link);
    }

    public Fanfiction build() {
        if (authors == null) {
            authors = List.of();
        }
        if (tags == null) {
            tags = List.of();
        }
        if (crossovers == null) {
            crossovers = List.of();
        }
        return new Fanfiction(title, chapters, words, language, rating,
                warningNoneGiven, warningNoneApply, warningViolence, warningRape, warningDeath, warningRape,
                catFf, catFm, catMm, catGen, catMulti, catOther,
                completed, lastUpdated, lastChecked, link, authors, tags, crossovers);
    }

    public FanfictionBuilder title(final String title) {
        this.title = Objects.requireNonNull(title);
        return this;
    }

    public String title() {
        return title;
    }

    public FanfictionBuilder chapters(final int chapters) {
        if (chapters <= 0) {
            throw new IllegalArgumentException("Number of chapters must be positive!");
        }
        this.chapters = chapters;
        return this;
    }

    public int chapters() {
        return chapters;
    }

    public FanfictionBuilder words(final int words) {
        if (words <= 0) {
            throw new IllegalArgumentException("Number of words must be positive!");
        }
        this.words = words;
        return this;
    }

    public int words() {
        return words;
    }

    public FanfictionBuilder language(final String language) {
        this.language = language;
        return this;
    }

    public String language() {
        return language;
    }

    public FanfictionBuilder rating(final String rating) {
        this.rating = rating;
        return this;
    }

    public String rating() {
        return rating;
    }

    public FanfictionBuilder warningNoneGiven(final boolean warningNoneGiven) {
        this.warningNoneGiven = warningNoneGiven;
        return this;
    }

    public boolean warningNoneGiven() {
        return warningNoneGiven;
    }

    public FanfictionBuilder warningNoneApply(final boolean warningNoneApply) {
        this.warningNoneApply = warningNoneApply;
        return this;
    }

    public boolean warningNoneApply() {
        return warningNoneApply;
    }

    public FanfictionBuilder warningViolence(final boolean warningViolence) {
        this.warningViolence = warningViolence;
        return this;
    }

    public boolean warningViolence() {
        return warningViolence;
    }

    public FanfictionBuilder warningRape(final boolean warningRape) {
        this.warningRape = warningRape;
        return this;
    }

    public boolean warningRape() {
        return warningRape;
    }

    public FanfictionBuilder warningDeath(final boolean warningDeath) {
        this.warningDeath = warningDeath;
        return this;
    }

    public boolean warningDeath() {
        return warningDeath;
    }

    public FanfictionBuilder warningUnderage(final boolean warningUnderage) {
        this.warningUnderage = warningUnderage;
        return this;
    }

    public boolean warningUnderage() {
        return warningUnderage;
    }

    public FanfictionBuilder catFf(final boolean catFf) {
        this.catFf = catFf;
        return this;
    }

    public boolean catFf() {
        return catFf;
    }

    public FanfictionBuilder catFm(final boolean catFm) {
        this.catFm = catFm;
        return this;
    }


    public boolean catFm() {
        return catFm;
    }
    public FanfictionBuilder catMm(final boolean catMm) {
        this.catMm = catMm;
        return this;
    }

    public boolean catMm() {
        return catMm;
    }

    public FanfictionBuilder catGen(final boolean catGen) {
        this.catGen = catGen;
        return this;
    }

    public boolean catGen() {
        return catGen;
    }

    public FanfictionBuilder catMulti(final boolean catMulti) {
        this.catMulti = catMulti;
        return this;
    }

    public boolean catMulti() {
        return catMulti;
    }

    public FanfictionBuilder catOther(final boolean catOther) {
        this.catOther = catOther;
        return this;
    }

    public boolean catOther() {
        return catOther;
    }

    public FanfictionBuilder completed(final boolean completed) {
        this.completed = completed;
        return this;
    }

    public boolean completed() {
        return completed;
    }

    public FanfictionBuilder lastUpdated(final Instant lastUpdated) {
        this.lastUpdated = Objects.requireNonNull(lastUpdated);
        return this;
    }

    public Instant lastUpdated() {
        return lastUpdated;
    }

    public FanfictionBuilder lastChecked(final Instant lastChecked) {
        this.lastChecked = lastChecked;
        return this;
    }

    public Instant lastChecked() {
        return lastChecked;
    }

    public FanfictionBuilder link(final String link) {
        this.link = Objects.requireNonNull(link);
        return this;
    }

    public String link() {
        return link;
    }

    public FanfictionBuilder authors(final List<Author> authors) {
        this.authors = List.copyOf(authors);
        return this;
    }

    public List<Author> authors() {
        return authors;
    }

    public FanfictionBuilder author(final Author author) {
        this.authors = List.of(author);
        return this;
    }

    public FanfictionBuilder tags(final List<Tag> tags) {
        this.tags = List.copyOf(tags);
        return this;
    }

    public List<Tag> tags() {
        return tags;
    }

    public FanfictionBuilder tag(final Tag tag) {
        this.tags = List.of(tag);
        return this;
    }

    public FanfictionBuilder crossovers(final List<Fandom> crossovers) {
        this.crossovers = List.copyOf(crossovers);
        return this;
    }

    public List<Fandom> crossovers() {
        return crossovers;
    }

    public FanfictionBuilder crossover(final Fandom crossover) {
        this.crossovers = List.of(crossover);
        return this;
    }

}
