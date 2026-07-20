package com.iti.presentation.staticcontent.state

sealed interface StaticContentIntent {

    data object Retry : StaticContentIntent

    data object BackClicked : StaticContentIntent
}
