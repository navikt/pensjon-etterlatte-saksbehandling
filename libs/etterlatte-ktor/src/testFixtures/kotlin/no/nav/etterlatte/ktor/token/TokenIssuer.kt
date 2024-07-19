package no.nav.etterlatte.ktor.token

import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Issuer
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

const val CLIENT_ID = "CLIENT_ID for saksbehandler"

fun MockOAuth2Server.issueSaksbehandlerToken(
    navn: String = "Navn Navnesen",
    navIdent: String = "Saksbehandler01",
    groups: List<String> = listOf(),
): String =
    this
        .issueToken(
            issuerId = Issuer.AZURE.issuerName,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    "navn" to navn,
                    Claims.NAVident.name to navIdent,
                    Claims.groups.name to groups,
                ),
        ).serialize()

fun MockOAuth2Server.issueSystembrukerToken(
    mittsystem: String = UUID.randomUUID().toString(),
    roles: List<String> = listOf(),
): String =
    this
        .issueToken(
            issuerId = Issuer.AZURE.issuerName,
            audience = CLIENT_ID,
            claims =
                mapOf(
                    Claims.azp_name.name to mittsystem,
                    "roles" to roles,
                    Claims.idtyp.name to "app",
                ),
        ).serialize()
