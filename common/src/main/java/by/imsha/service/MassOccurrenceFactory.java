package by.imsha.service;

import by.imsha.domain.Mass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MassOccurrenceFactory {
    private final MassCalendarMapper massCalendarMapper;

    public Stream<Occurrence> occurrences(Mass mass, Instant from, Instant toExclusive) {
        if (mass.isDeleted()) {
            return Stream.empty();
        }
        return massCalendarMapper.toScheduleRule(mass.getCalendar()).occurrences(from, toExclusive)
                .map(start -> new Occurrence(mass, start));
    }

    public record Occurrence(Mass mass, Instant start) {
    }
}
