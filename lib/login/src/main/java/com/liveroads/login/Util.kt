package com.liveroads.login

internal fun isValidEmail(cs: CharSequence?): Boolean {
    if (cs == null || cs.indexOf('@') < 0) {
        return false
    }
    val s = cs.toString().trim()
    val index = s.indexOf('@')
    return (index > 0 && index < s.length - 1)
}

internal fun isValidPassword(cs: CharSequence?): Boolean {
    return (cs != null && cs.length >= 8)
}
