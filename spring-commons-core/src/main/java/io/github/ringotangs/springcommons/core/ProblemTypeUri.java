package io.github.ringotangs.springcommons.core;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

/** Creates stable URNs for Problem Details types. */
public final class ProblemTypeUri {

    private static final String PREFIX = "urn:problem";
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private ProblemTypeUri() {}

    /**
     * Creates a problem type URI from a domain and one or more kebab-case segments.
     *
     * @param domain the problem domain
     * @param segments one or more problem type segments
     * @return the problem type URI
     * @throws NullPointerException if the domain, segment array, or any segment is null
     * @throws IllegalArgumentException if no problem segment is supplied or a segment is invalid
     */
    public static URI of(String domain, String... segments) {
        validateSegment(Objects.requireNonNull(domain, "domain must not be null"), "domain");
        Objects.requireNonNull(segments, "segments must not be null");
        if (segments.length == 0) {
            throw new IllegalArgumentException("at least one problem segment is required");
        }

        StringBuilder value = new StringBuilder(PREFIX).append(':').append(domain);
        for (int index = 0; index < segments.length; index++) {
            String segment = Objects.requireNonNull(segments[index], "segment must not be null");
            validateSegment(segment, "segment");
            value.append(':').append(segment);
        }
        return URI.create(value.toString());
    }

    private static void validateSegment(String value, String name) {
        if (!SEGMENT_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must contain only lowercase letters, digits, and single hyphens: " + value);
        }
    }
}
