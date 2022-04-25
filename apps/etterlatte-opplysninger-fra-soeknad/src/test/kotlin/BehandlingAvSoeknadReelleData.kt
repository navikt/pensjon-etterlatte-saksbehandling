import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.BearerTokens
import io.ktor.client.features.auth.providers.bearer
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import no.nav.etterlatte.behandlingfrasoknad.BehandlingsService
import no.nav.etterlatte.behandlingfrasoknad.StartBehandlingAvSoeknad
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class BehandlingAvSoeknadReelleData {
    @Test @Disabled
    fun test() {
        val rapid = TestRapid()
        val behandlingservice = BehandlingsService(HttpClient {
            install(JsonFeature) { serializer = JacksonSerializer { registerModule(JavaTimeModule()) } }
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(
                            accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6ImVuLWFwcCIsIm9pZCI6ImVuLWFwcCIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiTkFWaWRlbnQiOiJTYWtzYmVoYW5kbGVyMDEiLCJyb2xlcyI6WyJrYW4tc2V0dGUta2lsZGUiXX0.2ftwnoZiUfUa_J6WUkqj_Wdugb0CnvVXsEs-JYnQw_g",
                            refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhenVyZSIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjIsIk5BVmlkZW50IjoiU2Frc2JlaGFuZGxlcjAxIn0.GOkpURd2cKRjX5V0lTA-ZApk8E05VOUcAMvJ0RE_2r4"
                        )
                    }
                }
            }
        }, "http://localhost:8080")

        StartBehandlingAvSoeknad(rapid, behandlingservice)
        val hendelseJson = javaClass.getResource("/fullMessage3.json")!!.readText()
        rapid.sendTestMessage(hendelseJson)
        Assertions.assertEquals(1, rapid.inspektør.size)
        Assertions.assertEquals(1, rapid.inspektør.message(0)["@sak_id"].longValue())
        Assertions.assertEquals(36, rapid.inspektør.message(0)["@behandling_id"].textValue().length)
    }
}