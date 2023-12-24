package org.abos.linker.core;

import java.util.Objects;

public record Fandom(String name, String link) {

    public Fandom(final String name, final String link) {
        this.name = Objects.requireNonNull(name);
        this.link = link;
    }
}
