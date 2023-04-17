package no.nav.etterlatte.libs.common

data class Route<T>(
    val url: String,
    val basisrute: String,
    val clazz: Class<T>
) {
    fun medBody(baseURL: String, f: () -> T) = Invocation("$baseURL/$basisrute/$url", f())
}

data class Invocation<T>(val url: String, val body: T)