package io.github.ringotangs.ringoboot.verification.limit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssueLimitBucketTest {

    @Test
    void createsImmutableMultiSegmentBucket() {
        List<String> source = new ArrayList<>(List.of("login", "user@example.com"));
        IssueLimitBucket bucket = new IssueLimitBucket(source);
        source.set(0, "changed");

        assertEquals(List.of("login", "user@example.com"), bucket.segments());
        assertThrows(
                UnsupportedOperationException.class, () -> bucket.segments().add("x"));
        assertFalse(bucket.toString().contains("user@example.com"));
    }

    @Test
    void rejectsMissingOrBlankSegments() {
        assertThrows(NullPointerException.class, () -> IssueLimitBucket.of((String[]) null));
        assertThrows(IllegalArgumentException.class, IssueLimitBucket::of);
        assertThrows(NullPointerException.class, () -> new IssueLimitBucket(Arrays.asList("a", null)));
        assertThrows(IllegalArgumentException.class, () -> IssueLimitBucket.of("a", " "));
    }
}
