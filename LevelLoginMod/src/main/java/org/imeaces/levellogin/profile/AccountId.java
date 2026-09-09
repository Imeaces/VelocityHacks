package org.imeaces.levellogin.profile;

public record AccountId(
        String authType,
        String subject
) {
}
