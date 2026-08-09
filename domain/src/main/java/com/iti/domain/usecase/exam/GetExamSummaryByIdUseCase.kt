package com.iti.domain.usecase.exam

import com.iti.domain.model.exam.ExamSummary
import com.iti.domain.repository.ExamRepository

class GetExamSummaryByIdUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(id: String): ExamSummary? {
        return repository.getById(id)
    }
}
