package org.abos.linker.core;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Fanfiction(String title, int chapters, int words, String language, String rating,
                         boolean warningNoneGiven, boolean warningNoneApply, boolean warningViolence, boolean warningRape, boolean warningDeath, boolean warningUnderage,
                         boolean catFf, boolean catFm, boolean catMm, boolean catGen, boolean catMulti, boolean catOther,
                         boolean completed, Instant lastUpdated, Instant lastChecked, String link,
                         List<Author> authors, List<Tag> tags, List<Fandom> crossovers) {

    public Fanfiction(final String title, final int chapters, final int words, final String language, final String rating,
                      final boolean warningNoneGiven, final boolean warningNoneApply, final boolean warningViolence, final boolean warningRape, final boolean warningDeath, final boolean warningUnderage,
                      final boolean catFf, final boolean catFm, final boolean catMm, final boolean catGen, final boolean catMulti, final boolean catOther,
                      final boolean completed, final Instant lastUpdated, final Instant lastChecked, final String link,
                      final List<Author> authors, final List<Tag> tags, final List<Fandom> crossovers) {
        this.title = Objects.requireNonNull(title);
        if (chapters <= 0) {
            throw new IllegalArgumentException("Number of chapters must be positive!");
        }
        this.chapters = chapters;

        if (words <= 0) {
            throw new IllegalArgumentException("Number of words must be positive!");
        }
        this.words = words;
        this.language = language; // null means English
        this.rating = rating; // null means not rated
        this.warningNoneGiven = warningNoneGiven;
        this.warningNoneApply = warningNoneApply;
        this.warningViolence = warningViolence;
        this.warningRape = warningRape;
        this.warningDeath = warningDeath;
        this.warningUnderage = warningUnderage;
        this.catFf = catFf;
        this.catFm = catFm;
        this.catMm = catMm;
        this.catGen = catGen;
        this.catMulti = catMulti;
        this.catOther = catOther;
        this.completed = completed;
        this.lastUpdated = lastUpdated;
        this.lastChecked = lastChecked == null ? Instant.now() : lastChecked;
        this.link = Objects.requireNonNull(link);
        this.authors = Objects.requireNonNull(authors); // empty list means Anon author
        this.tags = Objects.requireNonNull(tags);
        this.crossovers = Objects.requireNonNull(crossovers);
    }
}
