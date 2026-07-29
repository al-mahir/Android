package com.iti.presentation.sheikh.state

import com.iti.domain.model.Sheikh

internal fun List<Sheikh>.applyFilters(query: String, filter: SheikhFilter): List<Sheikh> =
    filter { sheikh ->
        filter.matches(sheikh.availability) &&
            (query.isBlank() || sheikh.name.contains(query, ignoreCase = true))
    }
