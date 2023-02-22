package no.nav.etterlatte.behandlingfrasoknad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadstypeOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.opplysningerfrasoknad.Opplysningsuthenter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpplysningsuthenterOmstillingsstoenadTest {

    companion object {
        val opplysninger = Opplysningsuthenter().lagOpplysningsListe(
            objectMapper.treeToValue(
                objectMapper.readTree(
                    javaClass.getResource("/omstillingsmelding.json")!!.readText()
                )!!["@skjema_info"]
            ),
            SoeknadType.OMSTILLINGSSTOENAD
        )
    }

    @Test
    fun `alle opplysninger skal ha innsender som kilde`() {
        val kilde = Grunnlagsopplysning.Privatperson(
            "03108718357",
            LocalDateTime.parse("2022-02-14T14:37:24.573612786").toInstant(
                ZoneOffset.UTC
            )
        )
        opplysninger.forEach {
            assertEquals(kilde.fnr, (it.kilde as Grunnlagsopplysning.Privatperson).fnr)
            assertEquals(kilde.mottatDato, (it.kilde as Grunnlagsopplysning.Privatperson).mottatDato)
        }
    }

    @Test
    fun `skal hente opplysning om avdoede`() {
        consumeSingle<AvdoedSoeknad>(Opplysningstype.AVDOED_SOEKNAD_V1)
            .apply {
                assertEquals("Bernt", fornavn)
                assertEquals("jakobsen", etternavn)
                assertEquals(PersonType.AVDOED, type)
                assertEquals("22128202440", foedselsnummer.value)
                assertEquals(LocalDate.of(2022, Month.JANUARY, 1), doedsdato)
                assertEquals("Norge", statsborgerskap)
                assertEquals(JaNeiVetIkke.NEI, utenlandsopphold.harHattUtenlandsopphold)
                assertEquals(JaNeiVetIkke.NEI, doedsaarsakSkyldesYrkesskadeEllerYrkessykdom)
            }
    }

    @Test
    fun `skal hente opplysning om soeknadstype`() {
        consumeSingle<SoeknadstypeOpplysning>(Opplysningstype.SOEKNADSTYPE_V1).apply {
            assertEquals(SoeknadType.OMSTILLINGSSTOENAD, this.type)
        }
    }

    private inline fun <reified T> consumeSingle(opplysningType: Opplysningstype) =
        opplysninger.filter { it.opplysningType == opplysningType }
            .also { assertEquals(1, it.size) }
            .first()
            .let {
                it.opplysning as T
            }
}