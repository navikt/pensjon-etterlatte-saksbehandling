package no.nav.etterlatte.behandling.selftest

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.KlageKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.PingResultDown
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ServiceStatus
import java.time.LocalTime

class SelfTestService(
    axsysKlient: AxsysKlient,
    navAnsattKlient: NavAnsattKlient,
    skjermingKlient: SkjermingKlient,
    grunnlagKlient: GrunnlagKlient,
    pdlTjenesterKlient: PdlTjenesterKlient,
    klageKlient: KlageKlient,
    tilbakekrevingKlient: TilbakekrevingKlient,
) {
    val externalServices: List<Pingable> =
        listOf(
            axsysKlient,
            navAnsattKlient,
            skjermingKlient,
            grunnlagKlient,
            pdlTjenesterKlient,
            klageKlient,
            tilbakekrevingKlient,
        )

    /**
     * Returns result of self-test in HTML format.
     */
    fun performSelfTestAndReportAsHtml(): String = htmlPage(htmlStatusRows(performSelfTest()))

    /**
     * Returns result of self-test in JSON format.
     */
    fun performSelfTestAndReportAsJson(): String {
        val pingResults = performSelfTest()
        val aggregateCode = getAggregateResult(pingResults.values).code()
        val checks = json(pingResults)
        return """{"application":"$APPLICATION_NAME","timestamp":"${now()}","aggregateResult":$aggregateCode,"checks":[$checks]}"""
    }

    protected fun now(): LocalTime = LocalTime.now()

    private fun performSelfTest(): Map<String, PingResult> =
        externalServices
            .map { runBlocking { it.ping() } } // TODO: pings kan kjøre async forbedring mulig her
            .associateBy { it.serviceName }

    private companion object {
        private const val APPLICATION_NAME = "pensjonskalkulator-backend"

        private fun getAggregateResult(pingResults: Collection<PingResult>) =
            pingResults
                .map { it.status }
                .firstOrNull { it == ServiceStatus.DOWN } ?: ServiceStatus.UP

        private fun json(resultsByService: Map<String, PingResult>) = resultsByService.values.joinToString { json(it) }

        private fun json(result: PingResult): String {
            val errorMessageEntry =
                when (result) {
                    is PingResultDown -> result.errorMessage
                    is PingResultUp -> ""
                }

            return """{"endpoint":"${result.endpoint}","description":"
                ${result.serviceName}"$errorMessageEntry,"result":${result.status.code()}}
                """.trimMargin()
        }

        private fun htmlPage(tableRows: String) =
            """
            <!DOCTYPE html>
            <html>
            <head>
            <title>$APPLICATION_NAME selvtest</title>
            <style type="text/css">
            table {border-collapse: collapse; font-family: Tahoma, Geneva, sans-serif;}
            table td {padding: 15px;}
            table thead th 
            {padding: 15px; background-color: #54585d; color: #ffffff; font-weight: bold; font-size: 13px; border: 1px solid #54585d;}
            table tbody td {border: 1px solid #dddfe1;}
            table tbody tr {background-color: #f9fafb;}
            table tbody tr:nth-child(odd) {background-color: #ffffff;}
            </style>
            </head>
            <body>
            <div>
            <table>
            <thead>
            <tr>
            <th>Tjeneste</th><th>Status</th><th>Informasjon</th><th>Endepunkt</th><th>Beskrivelse</th>
            </tr>
            </thead>
            <tbody>$tableRows</tbody>
            </table>
            </div>
            </body>
            </html>
            """.trimIndent()

        private fun htmlStatusRows(resultsByService: Map<String, PingResult>) =
            resultsByService.entries.joinToString(separator = "", transform = ::htmlRow)

        private fun htmlRow(entry: Map.Entry<String, PingResult>) = htmlRow(entry.value)

        private fun htmlRow(result: PingResult) =
            "<tr>${htmlCell(result.serviceName)}${htmlStatusCell(result.status)}${htmlCell(result.message)}" +
                "${htmlCell(result.endpoint)}</tr>"

        private fun htmlCell(content: String) = "<td>$content</td>"

        private fun htmlStatusCell(status: ServiceStatus) =
            """<td style="background-color:${status.color()};text-align:center;">${status.name}</td>"""
    }
}
