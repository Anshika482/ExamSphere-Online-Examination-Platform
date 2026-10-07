package com.examsphere.controller;

import com.examsphere.dto.ApiResponse;
import com.examsphere.dto.ExamDtos.ExamSummary;
import com.examsphere.entity.Exam;
import com.examsphere.entity.ExamStatus;
import com.examsphere.exception.ResourceNotFoundException;
import com.examsphere.repository.ExamRepository;
import com.examsphere.service.DtoMapper;
import com.examsphere.service.StudentService;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only catalogue of published exams for visitors. Exposes summaries only, never questions. */
@RestController
@RequestMapping("/api/public/exams")
public class PublicExamController {

    private final ExamRepository examRepository;

    public PublicExamController(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<ExamSummary>> list(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String sort) {
        List<ExamSummary> exams = examRepository
                .findAll(StudentService.publishedExams(q, category), StudentService.examSort(sort)).stream()
                .map(exam -> DtoMapper.toSummary(exam, null))
                .filter(summary -> !"CLOSED".equals(summary.availability()))
                .toList();
        return ApiResponse.ok(exams);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<ExamSummary> get(@PathVariable Long id) {
        Exam exam = examRepository.findWithInstructorById(id)
                .filter(e -> e.getStatus() == ExamStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found."));
        return ApiResponse.ok(DtoMapper.toSummary(exam, null));
    }
}
