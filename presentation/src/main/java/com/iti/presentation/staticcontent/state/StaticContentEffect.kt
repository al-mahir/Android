package com.iti.presentation.staticcontent.state

sealed interface StaticContentEffect {

    data object NavigateBack : StaticContentEffect
}
