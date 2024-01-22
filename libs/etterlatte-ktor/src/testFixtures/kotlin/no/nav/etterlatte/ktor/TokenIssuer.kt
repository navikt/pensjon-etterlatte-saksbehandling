package no.nav.etterlatte.ktor

import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.security.mock.oauth2.MockOAuth2Server

const val CLIENT_ID = "CLIENT_ID"

fun MockOAuth2Server.issueSaksbehandlerToken(): String =
    this.issueToken(
        issuerId = AZURE_ISSUER,
        audience = CLIENT_ID,
        claims =
            mapOf(
                "navn" to "Per Persson",
                "NAVident" to "Saksbehandler01",
            ),
    ).serialize()
