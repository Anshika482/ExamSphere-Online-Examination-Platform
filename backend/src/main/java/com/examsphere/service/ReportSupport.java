package com.examsphere.service;

import com.examsphere.dto.ReportDtos.ExamStat;
import java.util.List;

/** Converts the aggregate rows returned by the repositories into typed report objects. */
final class ReportSupport {

    private ReportSupport() {
    }

    static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    static int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    static double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }

    static double percent(long part, long whole) {
        return whole == 0 ? 0.0 : DtoMapper.round2(part * 100.0 / whole);
    }

    /** Row layout: examId, title, attempts, avg marks, max, min, avg percentage, passed, total marks. */
    static ExamStat toExamStat(Object[] row) {
        long attempts = toLong(row[2]);
        long passed = toLong(row[7]);
        return new ExamStat(toLong(row[0]), (String) row[1], attempts, DtoMapper.round2(toDouble(row[3])),
                toInt(row[4]), toInt(row[5]), DtoMapper.round2(toDouble(row[6])), passed, attempts - passed,
                percent(passed, attempts), percent(attempts - passed, attempts), toInt(row[8]));
    }

    static List<ExamStat> toExamStats(List<Object[]> rows) {
        return rows.stream().map(ReportSupport::toExamStat).toList();
    }

    /** Attempt-weighted average percentage across several exams. */
    static double weightedAveragePercentage(List<ExamStat> stats) {
        long attempts = stats.stream().mapToLong(ExamStat::attempts).sum();
        if (attempts == 0) {
            return 0.0;
        }
        double sum = stats.stream().mapToDouble(s -> s.averagePercentage() * s.attempts()).sum();
        return DtoMapper.round2(sum / attempts);
    }
}
