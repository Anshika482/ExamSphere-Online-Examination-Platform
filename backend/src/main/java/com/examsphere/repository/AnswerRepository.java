package com.examsphere.repository;

import com.examsphere.entity.Answer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByAttemptId(Long attemptId);

    /** Columns: questionId, times attempted, correct count. Only submitted attempts are counted. */
    @Query("""
            select a.question.id,
                   sum(case when a.selectedOption is not null then 1 else 0 end),
                   sum(case when a.correct = true then 1 else 0 end)
            from Answer a
            where a.attempt.exam.id = :examId and a.attempt.submitted = true
            group by a.question.id
            """)
    List<Object[]> questionStatsForExam(@Param("examId") Long examId);
}
