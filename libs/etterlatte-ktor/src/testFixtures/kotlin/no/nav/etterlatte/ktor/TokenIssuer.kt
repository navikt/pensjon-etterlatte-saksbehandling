package no.nav.etterlatte.ktor

import no.nav.etterlatte.libs.ktor.AZURE_ISSUER
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

internal const val CLIENT_ID = "CLIENT_ID for saksbehandler"

fun MockOAuth2Server.issueSaksbehandlerToken(
    navn: String = "Navn Navnesen",
    navIdent: String = "Saksbehandler01",
    groups: List<String> = listOf(),
): String =
    this
        .issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "navn" to navn,
                    Claims.NAVident.name to navIdent,
                    "groups" to groups,
                ),
        ).serialize()

fun MockOAuth2Server.issueSystembrukerToken(
    mittsystem: String = UUID.randomUUID().toString(),
    roles: List<String> = listOf(),
): String =
    this
        .issueToken(
            issuerId = AZURE_ISSUER,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "sub" to mittsystem,
                    "oid" to mittsystem,
                    "azp_name" to mittsystem,
                    "roles" to roles,
                ),
        ).serialize()
