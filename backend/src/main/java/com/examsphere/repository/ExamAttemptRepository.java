package com.examsphere.repository;

import com.examsphere.entity.AttemptStatus;
import com.examsphere.entity.ExamAttempt;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long>, JpaSpecificationExecutor<ExamAttempt> {

    /**
     * Row lock (SELECT ... FOR UPDATE). Two concurrent submit requests are
     * serialised here, so only the first one can evaluate the attempt.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.id = :id")
    Optional<ExamAttempt> findByIdForUpdate(@Param("id") Long id);

    Optional<ExamAttempt> findByExamIdAndStudentId(Long examId, Long studentId);

    @EntityGraph(attributePaths = "exam")
    List<ExamAttempt> findByStudentId(Long studentId);

    @Override
    @EntityGraph(attributePaths = {"exam", "student"})
    Page<ExamAttempt> findAll(Specification<ExamAttempt> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"exam", "student"})
    List<ExamAttempt> findAll(Specification<ExamAttempt> spec, Sort sort);

    @Query("select a.id from ExamAttempt a where a.status = :status and a.deadlineAt < :cutoff")
    List<Long> findIdsByStatusAndDeadlineBefore(@Param("status") AttemptStatus status, @Param("cutoff") LocalDateTime cutoff);

    boolean existsByExamId(Long examId);

    long countBySubmittedTrue();

    long countBySubmittedTrueAndPassedTrue();

    @Query("select avg(a.percentage) from ExamAttempt a where a.submitted = true")
    Double averagePercentage();

    @Query("select a.exam.id, count(a) from ExamAttempt a group by a.exam.id")
    List<Object[]> countAttemptsPerExam();

    /**
     * Per-exam aggregates over submitted attempts. Columns: examId, title, attempts,
     * avg marks, max marks, min marks, avg percentage, passed count, total marks.
     */
    @Query("""
            select e.id, e.title, count(a), avg(a.marksObtained), max(a.marksObtained), min(a.marksObtained),
                   avg(a.percentage), sum(case when a.passed = true then 1 else 0 end), e.totalMarks
            from ExamAttempt a join a.exam e
            where a.submitted = true and e.instructor.id = :instructorId
            group by e.id, e.title, e.totalMarks
            order by e.title
            """)
    List<Object[]> examStatsForInstructor(@Param("instructorId") Long instructorId);

    @Query("""
            select e.id, e.title, count(a), avg(a.marksObtained), max(a.marksObtained), min(a.marksObtained),
                   avg(a.percentage), sum(case when a.passed = true then 1 else 0 end), e.totalMarks
            from ExamAttempt a join a.exam e
            where a.submitted = true
            group by e.id, e.title, e.totalMarks
            order by e.title
            """)
    List<Object[]> examStatsForAll();

    @Query("""
            select e.id, e.title, count(a), avg(a.marksObtained), max(a.marksObtained), min(a.marksObtained),
                   avg(a.percentage), sum(case when a.passed = true then 1 else 0 end), e.totalMarks
            from ExamAttempt a join a.exam e
            where a.submitted = true and e.id = :examId
            group by e.id, e.title, e.totalMarks
            """)
    List<Object[]> examStatsForExam(@Param("examId") Long examId);

    /** Columns: studentId, name, email, attempts, avg percentage, passed count. */
    @Query("""
            select s.id, s.fullName, s.email, count(a), avg(a.percentage),
                   sum(case when a.passed = true then 1 else 0 end)
            from ExamAttempt a join a.student s
            where a.submitted = true
            group by s.id, s.fullName, s.email
            order by count(a) desc, s.fullName
            """)
    List<Object[]> studentStats(Pageable pageable);

    @Query("select a.percentage from ExamAttempt a where a.submitted = true and a.exam.id = :examId")
    List<Double> percentagesForExam(@Param("examId") Long examId);

    @Query("select a.submittedAt from ExamAttempt a where a.submitted = true and a.submittedAt >= :since")
    List<LocalDateTime> submissionTimesSince(@Param("since") LocalDateTime since);
}
