import io.ktor.client.HttpClient
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.BearerTokens
import io.ktor.client.features.auth.providers.bearer
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import no.nav.etterlatte.BehandlingsService
import no.nav.etterlatte.StartBehandlingAvSoeknad
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class BehandlingAvSoeknadReelleData {
    @Test
    fun test(){
        val rapid = TestRapid()
        val behandlingservice = BehandlingsService(HttpClient {
            install(JsonFeature) { serializer = JacksonSerializer() }
            install(Auth) {
                bearer {
                    loadTokens {
                BearerTokens(
                    accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjIsIk5BVmlkZW50IjoiU2Frc2JlaGFuZGxlcjAxIn0.GOkpURd2cKRjX5V0lTA-ZApk8E05VOUcAMvJ0RE_2r4",
                    refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjIsIk5BVmlkZW50IjoiU2Frc2JlaGFuZGxlcjAxIn0.GOkpURd2cKRjX5V0lTA-ZApk8E05VOUcAMvJ0RE_2r4"
                )
            }}}
        }, "http://localhost:8080")

        StartBehandlingAvSoeknad(rapid, behandlingservice)
        val hendelseJson = javaClass.getResource("/fullMessage2.json")!!.readText()

        rapid.sendTestMessage(hendelseJson)

        Assertions.assertEquals(1, rapid.inspektør.size)
        Assertions.assertEquals(1, rapid.inspektør.message(0)["@sak_id"].longValue())

    }
}