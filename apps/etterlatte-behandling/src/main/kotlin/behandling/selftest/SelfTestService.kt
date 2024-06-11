package no.nav.etterlatte.behandling.selftest

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ServiceStatus
import java.time.LocalTime

class SelfTestService(
    val externalServices: List<Pingable>,
) {
    /**
     * Returns result of self-test in HTML format.
     */
    suspend fun performSelfTestAndReportAsHtml(): String = htmlPage(htmlStatusRows(performSelfTest()))

    suspend fun performSelfTestAndReportAsJson(): SelfTestReport {
        val pingResults = performSelfTest()
        val numberOfServicesDown = getAggregateResult(pingResults).code()

        return SelfTestReport(
            application = APPLICATION_NAME,
            timestamp = LocalTime.now(),
            aggregateResult = numberOfServicesDown,
            checks = pingResults,
        )
    }

    private suspend fun performSelfTest(): List<PingResult> =
        coroutineScope {
            externalServices
                .map { async { it.ping() } }
                .map { it.await() }
        }

    private companion object {
        private const val APPLICATION_NAME = "etterlatte-behandling"

        private fun getAggregateResult(pingResults: Collection<PingResult>) =
            pingResults
                .map { it.result }
                .firstOrNull { it == ServiceStatus.DOWN } ?: ServiceStatus.UP

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

        private fun htmlStatusRows(resultsByService: List<PingResult>) =
            resultsByService.joinToString(separator = "", transform = ::htmlRow)

        private fun htmlRow(result: PingResult) =
            "<tr>${htmlCell(result.serviceName)}${htmlStatusCell(result.result)}${htmlCell(result.description)}" +
                "${htmlCell(result.endpoint)}</tr>"

        private fun htmlCell(content: String) = "<td>$content</td>"

        private fun htmlStatusCell(status: ServiceStatus) =
            """<td style="background-color:${status.codeToColour()};text-align:center;">${status.name}</td>"""
    }
}
