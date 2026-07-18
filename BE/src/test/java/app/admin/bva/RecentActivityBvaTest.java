package app.admin.bva;

import app.admin.dto.response.RecentActivityResponse;
import app.admin.repository.RecentActivityRepository;
import app.admin.service.RecentActivityService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecentActivityBvaTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-17T12:00:00Z");

    @Mock
    private RecentActivityRepository repository;

    static Stream<Arguments> limitBoundaryCases() {
        return Stream.of(
                arguments("TC24 limit = -1", -1, 5),
                arguments("TC25 limit = 0", 0, 5),
                arguments("TC26 limit = 1", 1, 1)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("limitBoundaryCases")
    void normalizeRecentActivityLimit(String testCase, int inputLimit, int repositoryLimit) {
        when(repository.findRecentApplicationActivities(repositoryLimit)).thenReturn(List.of());
        RecentActivityService service = new RecentActivityService(repository);

        List<RecentActivityResponse> result = service.getRecentActivities(inputLimit);

        assertEquals(0, result.size());
        verify(repository).findRecentApplicationActivities(repositoryLimit);
    }

    static Stream<Arguments> databaseSizeCases() {
        return Stream.of(
                arguments("TC27 DB có N = 4", 4, 4),
                arguments("TC28 DB có N = 5", 5, 5),
                arguments("TC29 DB có N = 6", 6, 5)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databaseSizeCases")
    void mapNumberOfRowsReturnedByLimitedQuery(String testCase, int sourceSize, int expectedSize) {
        when(repository.findRecentApplicationActivities(5)).thenReturn(activityRows(expectedSize));
        RecentActivityService service = new RecentActivityService(repository);

        List<RecentActivityResponse> result = service.getRecentActivities(5);

        assertEquals(expectedSize, result.size());
        verify(repository).findRecentApplicationActivities(5);
    }

    static Stream<Arguments> timeAgoBoundaryCases() {
        return Stream.of(
                arguments("TC30 timeAgo 59 giây", 59L, "Vừa xong"),
                arguments("TC31 timeAgo 60 giây", 60L, "1 phút trước"),
                arguments("TC32 timeAgo 59 phút 59 giây", 3_599L, "59 phút trước"),
                arguments("TC33 timeAgo 60 phút", 3_600L, "1 giờ trước"),
                arguments("TC34 timeAgo 23 giờ 59 phút 59 giây", 86_399L, "23 giờ trước"),
                arguments("TC35 timeAgo 24 giờ", 86_400L, "1 ngày trước")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("timeAgoBoundaryCases")
    void formatTimeAgoAtBoundaries(String testCase, long elapsedSeconds, String expected) {
        RecentActivityService service = new RecentActivityService(repository);
        Instant createdAt = FIXED_NOW.minusSeconds(elapsedSeconds);

        String actual = ReflectionTestUtils.invokeMethod(service, "toTimeAgoVi", createdAt, FIXED_NOW);

        assertEquals(expected, actual);
    }

    private static List<Object[]> activityRows(int size) {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            rows.add(new Object[]{
                    (long) i + 1,
                    "Candidate " + i,
                    "Company " + i,
                    Timestamp.from(FIXED_NOW.minusSeconds(120L + i))
            });
        }
        return rows;
    }
}
