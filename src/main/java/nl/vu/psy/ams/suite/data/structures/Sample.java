package nl.vu.psy.ams.suite.data.structures;

import java.util.Arrays;
import java.util.Objects;

public record Sample(long time, float[] data) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sample(long time1, float[] data1))) return false;
        return time == time1 && Arrays.equals(data, data1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, Arrays.hashCode(data));
    }
}
