package io.github.mfedenko09.roastlands.content;

import java.util.List;

public record VerificationReport(int passed, int failed, List<String> failures) {

    public VerificationReport {
        failures = List.copyOf(failures);
    }

    public boolean isSuccess() {
        return failed == 0;
    }

    public String summary() {
        return passed + " passed, " + failed + " failed";
    }
}
