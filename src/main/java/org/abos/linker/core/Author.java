package org.abos.linker.core;

import org.abos.common.Named;

import java.util.List;
import java.util.Objects;

public record Author(String name, List<String> links) implements Named {

    public Author(final String name, final List<String> links) {
        this.name = Objects.requireNonNull(name);
        this.links = Objects.requireNonNull(links);
    }

    @Override
    public String getName() {
        return name();
    }
}
