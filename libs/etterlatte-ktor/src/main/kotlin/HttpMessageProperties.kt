package no.nav.etterlatte.libs.ktor

import io.ktor.client.request.header
import io.ktor.http.HttpMessageBuilder
import no.nav.etterlatte.libs.common.behandling.SakType

private object Headers {
    const val BEHANDLINGSNUMMER = "behandlingsnummer"
    const val NAV_CONSUMER_ID = "Nav-Consumer-Id"
}

fun HttpMessageBuilder.behandlingsnummer(vararg sakType: SakType): Unit =
    header(Headers.BEHANDLINGSNUMMER, sakType.joinToString { it.behandlingsnummer })

fun HttpMessageBuilder.behandlingsnummer(sakTyper: List<SakType>): Unit =
    header(Headers.BEHANDLINGSNUMMER, sakTyper.joinToString { it.behandlingsnummer })

fun HttpMessageBuilder.navConsumerId(applicationName: String) = header(Headers.NAV_CONSUMER_ID, applicationName)
