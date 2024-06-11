package no.nav.etterlatte.libs.common.behandling

import io.kotest.matchers.equals.shouldBeEqual
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class KlageUtfallMedDataTest {
    private val tidspunkt = Tidspunkt.ofNorskTidssone(LocalDate.of(2024, 4, 5), LocalTime.of(19, 45))

    @Test
    fun toJson() {
        KlageUtfallMedData
            .Omgjoering(
                KlageOmgjoering(GrunnForOmgjoering.FEIL_LOVANVENDELSE, "abc"),
                Grunnlagsopplysning.Saksbehandler("UB40", tidspunkt),
            ).toJson() shouldBeEqual
            """                    
            {"utfall":"OMGJOERING","omgjoering":{"grunnForOmgjoering":"FEIL_LOVANVENDELSE","begrunnelse":"abc"},
            "saksbehandler":{"ident":"UB40","tidspunkt":"2024-04-05T17:45:00Z","type":"saksbehandler"}}
            """.trimIndent().replace("\n", "")
    }

    @Test
    fun fromJson() {
        val json =
            """             
                   {"utfall":"AVVIST",
                   "saksbehandler":{"ident":"UB40","tidspunkt":"2024-04-05T17:45:00Z","type":"saksbehandler"},
                   "vedtak":{"vedtakId":13},
                   "brev":{"brevId":123}}
                """.replace("\n", "").trimIndent()

        deserialize<KlageUtfallMedData>(json) shouldBeEqual
            KlageUtfallMedData.Avvist(
                Grunnlagsopplysning.Saksbehandler("UB40", tidspunkt),
                KlageVedtak(13L),
                KlageVedtaksbrev(123L),
            )
    }
}
