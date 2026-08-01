package com.iti.domain.exceptions

class NoConnectionException(
    message: String = "No internet connection"
) : Exception(message)
