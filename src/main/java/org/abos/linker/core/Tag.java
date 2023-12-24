package org.abos.linker.core;

import java.util.Objects;

public record Tag(String name, String description, boolean isCharacter, boolean isRelationship, String fandom, String link) {

    public static final Tag DUMMY = new Tag("", null, false, false, null, null);

    /**
     * Creates a new {@link Tag} instance.
     * @param name the name of the tag, not {@code null}
     * @param description a short description of the tag, may be {@code null}
     * @param isCharacter if this tag represents a character, mutually exclusive with {@code isRelationship}
     * @param isRelationship if this tag represents a relationship, mutually exclusive with {@code isCharacter}
     * @param fandom a fandom associated with the tag, may be {@code null}
     * @param link a link to a page further explaining the tag, may be {@code null}
     */
    public Tag(final String name, final String description,
               final boolean isCharacter, final boolean isRelationship,
               final String fandom, final String link) {
        this.name = Objects.requireNonNull(name);
        this.description = description;
        if (isCharacter && isRelationship) {
            throw new IllegalArgumentException("A tag cannot represent a character and relationship at the same time!");
        }
        this.isCharacter = isCharacter;
        this.isRelationship = isRelationship;
        this.fandom = fandom;
        this.link = link;
    }

}
