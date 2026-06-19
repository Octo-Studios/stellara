package it.hurts.shatterbyte.stellara.client.sky.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SkyboxRegistry {
    private static final List<SkyboxEntry> ENTRIES = new ArrayList<>();
    private static final List<SkyboxEntry> DATA_ENTRIES = new ArrayList<>();

    public static void register(SkyboxContent content, SkyboxCondition condition) {
        register(content, condition, 0.08F);
    }

    public static void register(SkyboxContent content, SkyboxCondition condition, float fadeSpeed) {
        ENTRIES.add(new SkyboxEntry(content, condition, fadeSpeed, 0));

        sort(ENTRIES);
    }

    public static void replaceDataEntries(List<SkyboxEntry> entries) {
        DATA_ENTRIES.clear();
        DATA_ENTRIES.addAll(entries);

        sort(DATA_ENTRIES);
    }

    public static List<SkyboxEntry> entries() {
        var entries = new ArrayList<SkyboxEntry>(ENTRIES.size() + DATA_ENTRIES.size());

        entries.addAll(ENTRIES);
        entries.addAll(DATA_ENTRIES);

        sort(entries);

        return List.copyOf(entries);
    }

    private static void sort(List<SkyboxEntry> entries) {
        entries.sort(Comparator.comparingInt(entry -> entry.content().priority()));
    }
}
