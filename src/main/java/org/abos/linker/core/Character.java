package org.abos.linker.core;

import java.util.Objects;

public record Character(String name, String description, String fandom, String link) {

    public static final Character DUMMY = new Character("", "", null, null);

    /**
     * Creates a new {@link Character} instance.
     * @param name the name of the character, not {@code null}
     * @param description a short description of the character, not {@code null}
     * @param fandom the fandom of the character, may be {@code null}
     * @param link a link to the character's page, may be {@code null}
     */
    public Character(final String name, final String description, final String fandom, final String link) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.fandom = fandom;
        this.link = link;
    }

}
