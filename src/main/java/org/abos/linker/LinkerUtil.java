package org.abos.linker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class LinkerUtil {

    private LinkerUtil() {
        /* No instantiation. */
    }

    public static int count(final Map<String, Integer> map, final String startString) {
        final int[] count = new int[] {0};
        map.forEach((name, value) -> {
            if (name.startsWith(startString)) {
                count[0] += value;
            }
        });
        return count[0];
    }

    public static String toCsvString(final Map<?, ?> map) {
        final StringBuilder s = new StringBuilder();
        map.forEach((key, val) -> {
            s.append(key);
            s.append(';');
            s.append(val);
            s.append("\r\n");
        });
        return s.toString();
    }

    public static void createCsvFromUploadTimes(final Map<Integer, ZonedDateTime> uploadTimes, final String fileLocation) throws IOException {
        final Map<Integer, LocalDate> simplifiedTimes = new HashMap<>();
        final ZoneId utc = ZoneId.of("UTC");
        // simplify the map
        for (var entry : uploadTimes.entrySet()) {
            simplifiedTimes.put(entry.getKey(), entry.getValue().withZoneSameInstant(utc).toLocalDate());
        }
        final Comparator<LocalDate> dateComp = Comparator.comparing(DateTimeFormatter.ISO_LOCAL_DATE::format);
        final LocalDate minDate = simplifiedTimes.values().stream().min(dateComp).get();
        // we want maxDate to be exclusive
        final LocalDate maxDate = simplifiedTimes.values().stream().max(dateComp).get().plusDays(1);
        final Map<LocalDate, Integer> uploadsPerDay = new HashMap<>();
        // populate the map
        for (LocalDate currentDate = minDate; currentDate.isBefore(maxDate); currentDate = currentDate.plusDays(1)) {
            uploadsPerDay.put(currentDate, 0);
        }
        // count uploads
        for (var entry : simplifiedTimes.entrySet()) {
            uploadsPerDay.put(entry.getValue(), uploadsPerDay.get(entry.getValue()) + 1);
        }
        // write CSV
        int sum = 0;
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileLocation))) {
            for (LocalDate currentDate = minDate; currentDate.isBefore(maxDate); currentDate = currentDate.plusDays(1)) {
                final int uploads = uploadsPerDay.get(currentDate);
                sum += uploads;
                bw.write(DateTimeFormatter.ISO_LOCAL_DATE.format(currentDate));
                bw.write(",");
                bw.write(Integer.toString(uploads));
                bw.write(",");
                bw.write(Integer.toString(sum));
                bw.write("\r\n"); // we want to ensure it is readable in Windows Text Editor
            }
            bw.flush();
        }
    }

}
