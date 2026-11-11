package com.dulo.domain.tree;

import com.dulo.domain.shared.Ids;

import java.util.Objects;
import java.util.UUID;

public record TreeId(UUID value) {

    public TreeId {
        Objects.requireNonNull(value, "value");
    }

    public static TreeId newId() {
        return new TreeId(Ids.newId());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
