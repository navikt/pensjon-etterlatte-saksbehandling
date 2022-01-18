package behandlingfrasoknad

import no.nav.etterlatte.behandlingfrasoknad.Opplysningsuthenter
import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Opplysning
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class OpplysningsuthenterTest {

    @Test
    fun lagSkjemaInfoOpplysning() {
        val hendelseJson = objectMapper.readTree(javaClass.getResource("/fullMessage2.json")!!.readText())
        val opplysningsuthenter = Opplysningsuthenter()
        val opplysning = opplysningsuthenter.lagSkjemaInfoOpplysning(hendelseJson["@skjema_info"])

        assertEquals((opplysning.kilde as Opplysning.Privatperson).fnr, "09018701453")
        assertEquals(
            (opplysning.kilde as Opplysning.Privatperson).mottatDato.epochSecond,
            LocalDateTime.parse("2022-01-03T13:44:25.888888401").atOffset(
                ZoneOffset.UTC
            ).toInstant().epochSecond
        )

    }
}