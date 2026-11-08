package hospital.validation;

import java.util.Set;

public final class ReceptionStatusFilter {
    private static final Set<String> STATUSES = Set.of(
            "BOOKED", "CHECKED_IN", "IN_CONSULTATION", "COMPLETED", "CANCELLED", "NEEDS_PROCESSING");

    private ReceptionStatusFilter() { }

    public static String normalize(String status) {
        if (status == null || status.isBlank()) return "";
        String normalized = status.trim();
        return STATUSES.contains(normalized) ? normalized : "";
    }
}
