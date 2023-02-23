package no.nav.etterlatte.token

data class AccessTokenWrapper(val accessToken: String, val oid: String?, val sub: String?)

enum class Claims {
    NAVident,
    oid, // ktlint-disable enum-entry-name-case
    sub // ktlint-disable enum-entry-name-case
}