package app.admin.repository;

import app.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

@Repository
public interface ApplicaionByDayRepository extends JpaRepository<User, Long> {

    @Query(value = """
        select date(a.applied_at) as d, count(*) as c
        from job_applications a
        where a.applied_at >= :from and a.applied_at < :to
        group by d
        order by d
    """, nativeQuery = true)
    List<Object[]> countApplicationsPerDay(@Param("from") Instant from, @Param("to") Instant to);
}
