package no.nav.etterlatte.ktor

import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

const val CLIENT_ID = "CLIENT_ID"

fun MockOAuth2Server.issueSaksbehandlerToken(
    navn: String = "Navn Navnesen",
    navIdent: String = "Saksbehandler01",
    groups: List<String> = listOf(),
): String =
    this.issueToken(
        issuerId = AZURE_ISSUER,
        audience = CLIENT_ID,
        claims =
            mapOf(
                "navn" to navn,
                "NAVident" to navIdent,
                "groups" to groups,
            ),
    ).serialize()

fun MockOAuth2Server.issueSystembrukerToken(): String {
    val mittsystem = UUID.randomUUID().toString()
    return this.issueToken(
        issuerId = AZURE_ISSUER,
        audience = CLIENT_ID,
        claims =
            mapOf(
                "sub" to mittsystem,
                "oid" to mittsystem,
            ),
    ).serialize()
}
