package app.admin.bva;

import app.admin.dto.response.ApplicationsByDayResponse;
import app.admin.repository.ApplicaionByDayRepository;
import app.admin.service.ApplicationByDay;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationByDayBvaTest {

    @Mock
    private ApplicaionByDayRepository repository;

    static Stream<Arguments> dayBoundaryCases() {
        return Stream.of(
                arguments("TC21 days = -1", -1, 7),
                arguments("TC22 days = 0", 0, 7),
                arguments("TC23 days = 1", 1, 1)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dayBoundaryCases")
    void returnExpectedNumberOfChartDays(String testCase, int days, int expectedSize) {
        when(repository.countApplicationsPerDay(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        ApplicationByDay service = new ApplicationByDay(repository);

        List<ApplicationsByDayResponse> result = service.getApplicationsChart(days);

        assertEquals(expectedSize, result.size());
        assertTrue(result.stream().allMatch(item -> item.getCount() == 0L));
        verify(repository).countApplicationsPerDay(any(Instant.class), any(Instant.class));
    }
}
