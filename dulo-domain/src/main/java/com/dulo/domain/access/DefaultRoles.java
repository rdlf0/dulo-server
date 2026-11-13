package com.dulo.domain.access;

import com.dulo.domain.feature.Feature;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The roles the initial setup proposes. The administrator can adjust them before saving. */
public final class DefaultRoles {

    public static final RoleKey ADMIN = new RoleKey("admin");
    public static final RoleKey EDITOR = new RoleKey("editor");
    public static final RoleKey CONTRIBUTOR = new RoleKey("contributor");
    public static final RoleKey VIEWER = new RoleKey("viewer");

    /** Features holding genealogy records, where every kind of access makes sense. */
    private static final Set<Feature> RECORDS =
            EnumSet.of(Feature.PEOPLE, Feature.FAMILIES, Feature.EVENTS, Feature.PLACES);

    /** Read-only features: views over the records. */
    private static final Set<Feature> VIEWS =
            EnumSet.of(Feature.TREE_VIEW, Feature.MAP_VIEW, Feature.TIMELINE);

    private DefaultRoles() {}

    /** Admin, editor, contributor and viewer, in that order. */
    public static List<Role> all() {
        return List.of(
                new Role(ADMIN, "Administrator", true, Permissions.all(), 0),
                new Role(EDITOR, "Editor", true, permissions(EnumSet.allOf(Access.class), true), 0),
                new Role(
                        CONTRIBUTOR,
                        "Contributor",
                        true,
                        permissions(EnumSet.of(Access.VIEW, Access.CREATE, Access.EDIT), false),
                        0),
                new Role(VIEWER, "Viewer", true, permissions(EnumSet.of(Access.VIEW), false), 0));
    }

    private static Permissions permissions(final Set<Access> onRecords, final boolean auditLog) {
        final Map<Feature, Set<Access>> grants = new EnumMap<>(Feature.class);
        RECORDS.forEach(f -> grants.put(f, onRecords));
        VIEWS.forEach(f -> grants.put(f, Set.of(Access.VIEW)));
        if (auditLog) {
            grants.put(Feature.AUDIT_LOG, Set.of(Access.VIEW));
        }
        return new Permissions(grants);
    }
}
