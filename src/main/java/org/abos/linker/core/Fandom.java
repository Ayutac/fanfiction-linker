package org.abos.linker.core;

import org.abos.common.Named;

import java.util.Objects;

public record Fandom(String name, String link) implements Named {

    public Fandom(final String name, final String link) {
        this.name = Objects.requireNonNull(name);
        this.link = link;
    }

    @Override
    public String getName() {
        return name();
    }
}
