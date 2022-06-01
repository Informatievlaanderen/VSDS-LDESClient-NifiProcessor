package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import java.time.LocalDateTime;

public class FragmentSettings {
    public static LocalDateTime IMMUTABLE = null;

    private final LocalDateTime expireDate;

    public FragmentSettings(LocalDateTime expireDate) {
        this.expireDate = expireDate;
    }

    public LocalDateTime getExpireDate() {
        return expireDate;
    }

    @Override
    public String toString() {
        return "FragmentSettings{" +
                "expireDate=" + expireDate +
                "}";
    }
}
