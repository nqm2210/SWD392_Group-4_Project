package hospital.validation;

public final class PatientLookupValidator {
    private PatientLookupValidator() { }

    public static boolean isValidQuery(String query) {
        if (query == null) return false;
        String value = query.trim();
        return !value.isEmpty() && value.length() <= 20 && value.matches("[A-Za-z0-9+(). -]+");
    }

    public static Integer patientIdCandidate(String query) {
        if (query == null || !query.trim().matches("[0-9]{1,10}")) return null;
        try {
            return Integer.valueOf(query.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
