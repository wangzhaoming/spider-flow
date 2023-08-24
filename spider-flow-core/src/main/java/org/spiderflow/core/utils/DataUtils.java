package org.spiderflow.core.utils;

import java.util.Collection;
import java.util.stream.Collectors;

public final class DataUtils {
    private DataUtils() {
    }

    public static Collection<Object> replaceNewlineCharInString(Collection<Object> in) {
        return in.stream().map(item -> {
            if (item instanceof String) {
                item = ((String) item).replaceAll("\\n", "\\\\n");
            }
            return item;
        }).collect(Collectors.toList());
    }
}
