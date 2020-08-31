package org.sandboxpowered.bootstrap.util;

import java.util.Locale;

public enum Edition {
    FABRIC("Fabric");

    private final String name;
    private final String prefix;

    Edition(String name) {
        this(name, name.toLowerCase(Locale.ROOT));
    }

    Edition(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }
}
