package com.glance.codex.platform.paper.config.engine.reload;

import java.util.List;

public record ReloadSummary(int ok, List<String> errors) {
    public static ReloadSummary oneOk() {
        return new ReloadSummary(1, List.of());
    }

    public static ReloadSummary oneFail(String err) {
        return new ReloadSummary(0, List.of(err));
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("───── Config Reload Summary ─────\n");
        sb.append("Successful: ").append(ok()).append("\n");
        sb.append("Failed: ").append(errors().size()).append("\n");

        if (!errors().isEmpty()) {
            sb.append("Errors:\n");
            for (String err : errors()) {
                sb.append("  - ").append(err).append("\n");
            }
        }

        sb.append("────────────────────────");
        return sb.toString();
    }
}
