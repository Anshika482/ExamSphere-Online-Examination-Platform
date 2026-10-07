package com.examsphere.repository;

import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long>, JpaSpecificationExecutor<Exam> {

    /** Loads the instructor in the same query, which avoids an N+1 on list screens. */
    @Override
    @EntityGraph(attributePaths = "instructor")
    List<Exam> findAll(Specification<Exam> spec, Sort sort);

    @Query("select e from Exam e join fetch e.instructor where e.id = :id")
    Optional<Exam> findWithInstructorById(@Param("id") Long id);

    List<Exam> findByInstructorIdOrderByCreatedAtDesc(Long instructorId);

    long countByStatus(ExamStatus status);

    @Query("select distinct e.category from Exam e where e.status = com.examsphere.entity.ExamStatus.PUBLISHED order by e.category")
    List<String> findPublishedCategories();

    @Query("select distinct e.category from Exam e order by e.category")
    List<String> findAllCategories();
}
