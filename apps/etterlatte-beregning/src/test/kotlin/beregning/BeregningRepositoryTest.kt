package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import java.util.UUID
import java.util.UUID.randomUUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BeregningRepositoryTest(
    ds: DataSource,
) {
    private val beregningRepository = BeregningRepository(ds)

    @Test
    fun `lagre() skal returnere samme data som faktisk ble lagret`() {
        val beregning = beregning()
        val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)

        assertEquals(beregning, lagretBeregning)
    }

    @Test
    fun `lagre() skal returnere samme data som faktisk ble lagret - teoretisk trygdetid`() {
        val beregning =
            beregning(
                beregningsMetode = BeregningsMetode.PRORATA,
                samletTeoretiskTrygdetid = 12,
                broek = IntBroek(1, 2),
            )
        val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)

        assertEquals(beregning, lagretBeregning)
    }

    @Test
    fun `det som hentes ut skal vaere likt det som originalt ble lagret`() {
        val beregningLagret = beregning()
        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)

        val beregningHentet = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(beregningLagret, beregningHentet)
    }

    @Test
    fun `skal oppdatere og eller lagre beregning`() {
        val beregningLagret = beregning()

        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)
        val beregningHentet = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(beregningLagret, beregningHentet)

        val nyBeregning = beregning(beregningLagret.behandlingId, YearMonth.of(2022, 2))

        beregningRepository.lagreEllerOppdaterBeregning(nyBeregning)
        val beregningHentetNy = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(nyBeregning, beregningHentetNy)
    }

    @Test
    fun `skal ikke hente en overstyr beregning som ikke finnes`() {
        val overstyrBeregning = beregningRepository.hentOverstyrBeregning(0L)

        assertTrue(overstyrBeregning == null)
    }

    @Test
    fun `skal ikke hente en overstyr beregning som har status ugyldig`() {
        beregningRepository.opprettOverstyrBeregning(OverstyrBeregning(10L, "Test", Tidspunkt.now(), OverstyrBeregningStatus.IKKE_AKTIV))
        val beregningLagret = beregning()
        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)
        val overstyrBeregning = beregningRepository.hentOverstyrBeregning(10L)

        assertEquals(overstyrBeregning, null)
    }

    @Test
    fun `skal kunne opprette en gyldig overstyr beregning etter en ugyldig`() {
        beregningRepository.opprettOverstyrBeregning(OverstyrBeregning(10L, "Test", Tidspunkt.now(), OverstyrBeregningStatus.IKKE_AKTIV))
        beregningRepository.lagreEllerOppdaterBeregning(beregning())
        assertEquals(null, beregningRepository.hentOverstyrBeregning(10L))

        val overstyrBeregning =
            beregningRepository.opprettOverstyrBeregning(
                OverstyrBeregning(10L, "Test", Tidspunkt.now(), OverstyrBeregningStatus.AKTIV),
            )
        assertEquals(overstyrBeregning, beregningRepository.hentOverstyrBeregning(10L))
    }

    @Test
    fun `skal lagre og hente en overstyr beregning`() {
        val opprettetOverstyrBeregning = beregningRepository.opprettOverstyrBeregning(OverstyrBeregning(1L, "Test", Tidspunkt.now()))

        val overstyrBeregning = beregningRepository.hentOverstyrBeregning(1L)

        assertNotNull(overstyrBeregning)

        assertEquals(opprettetOverstyrBeregning?.sakId, overstyrBeregning?.sakId)
        assertEquals(opprettetOverstyrBeregning?.beskrivelse, overstyrBeregning?.beskrivelse)
    }

    private fun beregning(
        behandlingId: UUID = randomUUID(),
        datoFOM: YearMonth = YearMonth.of(2021, 2),
        beregningsMetode: BeregningsMetode = BeregningsMetode.NASJONAL,
        samletTeoretiskTrygdetid: Int? = null,
        broek: IntBroek? = null,
        overstyrBeregning: OverstyrBeregning? = null,
    ) = Beregning(
        beregningId = randomUUID(),
        behandlingId = behandlingId,
        type = Beregningstype.BP,
        beregnetDato = Tidspunkt.now(),
        grunnlagMetadata =
            no.nav.etterlatte.libs.common.grunnlag
                .Metadata(1, 1),
        beregningsperioder =
            listOf(
                Beregningsperiode(
                    datoFOM = datoFOM,
                    datoTOM = null,
                    utbetaltBeloep = 3000,
                    soeskenFlokk = listOf(HELSOESKEN_FOEDSELSNUMMER.value),
                    institusjonsopphold = InstitusjonsoppholdBeregningsgrunnlag(Reduksjon.JA_VANLIG),
                    grunnbelopMnd = 10_000,
                    grunnbelop = 100_000,
                    trygdetid = 40,
                    beregningsMetode = beregningsMetode,
                    samletNorskTrygdetid = 40,
                    samletTeoretiskTrygdetid = samletTeoretiskTrygdetid,
                    broek = broek,
                    regelResultat = mapOf("regel" to "resultat").toObjectNode(),
                    regelVersjon = "1",
                    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
                ),
            ),
        overstyrBeregning = overstyrBeregning,
    )
}
