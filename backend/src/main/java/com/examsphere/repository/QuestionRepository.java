package com.examsphere.repository;

import com.examsphere.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByExamIdOrderByDisplayOrderAscIdAsc(Long examId);
}
