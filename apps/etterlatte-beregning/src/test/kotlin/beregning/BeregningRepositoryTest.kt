package no.nav.etterlatte.beregning

import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.beregning.OverstyrtBeregningKategori
import no.nav.etterlatte.libs.common.beregning.Regelverk
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

        assertEquals(beregning, lagretBeregning.fjernTilfeldigPeriodeIds())
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

        assertEquals(beregning, lagretBeregning.fjernTilfeldigPeriodeIds())
    }

    @Test
    fun `det som hentes ut skal vaere likt det som originalt ble lagret`() {
        val beregningLagret = beregning()
        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)

        val beregningHentet = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(beregningLagret, beregningHentet.fjernTilfeldigPeriodeIds())
    }

    @Test
    fun `skal oppdatere og eller lagre beregning`() {
        val beregningLagret = beregning()

        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)
        val beregningHentet = beregningRepository.hent(beregningLagret.behandlingId)

        assertTrue(beregningHentet!!.beregningsperioder.none { it.id == null })

        assertEquals(beregningLagret, beregningHentet.fjernTilfeldigPeriodeIds())

        val nyBeregning = beregning(beregningLagret.behandlingId, YearMonth.of(2022, 2))

        beregningRepository.lagreEllerOppdaterBeregning(nyBeregning)
        val beregningHentetNy = beregningRepository.hent(beregningLagret.behandlingId)

        assertEquals(nyBeregning, beregningHentetNy.fjernTilfeldigPeriodeIds())
    }

    @Test
    fun `skal ikke hente en overstyr beregning som ikke finnes`() {
        val overstyrBeregning = beregningRepository.hentOverstyrBeregning(randomSakId())

        assertTrue(overstyrBeregning == null)
    }

    @Test
    fun `skal ikke hente en overstyr beregning som har status ugyldig`() {
        val sakId = randomSakId()
        beregningRepository.opprettOverstyrBeregning(
            OverstyrBeregning(
                sakId,
                "Test",
                Tidspunkt.now(),
                OverstyrBeregningStatus.IKKE_AKTIV,
                kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI,
            ),
        )

        val beregningLagret = beregning()
        beregningRepository.lagreEllerOppdaterBeregning(beregningLagret)
        val overstyrBeregning = beregningRepository.hentOverstyrBeregning(sakId)

        assertEquals(overstyrBeregning, null)
    }

    @Test
    fun `skal kunne opprette en gyldig overstyr beregning etter en ugyldig`() {
        val sakId = randomSakId()

        beregningRepository.opprettOverstyrBeregning(
            OverstyrBeregning(
                sakId,
                "Test",
                Tidspunkt.now(),
                OverstyrBeregningStatus.IKKE_AKTIV,
                kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI,
            ),
        )

        beregningRepository.lagreEllerOppdaterBeregning(beregning())
        assertEquals(null, beregningRepository.hentOverstyrBeregning(sakId))

        val overstyrBeregning =
            beregningRepository.opprettOverstyrBeregning(
                OverstyrBeregning(sakId, "Test", Tidspunkt.now(), kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI),
            )
        assertEquals(overstyrBeregning, beregningRepository.hentOverstyrBeregning(sakId))
    }

    @Test
    fun `skal lagre og hente en overstyr beregning`() {
        val sakId = sakId1

        val opprettetOverstyrBeregning =
            beregningRepository.opprettOverstyrBeregning(
                OverstyrBeregning(sakId, "Test", Tidspunkt.now(), kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI),
            )

        val overstyrBeregning = beregningRepository.hentOverstyrBeregning(sakId)

        assertNotNull(overstyrBeregning)

        assertEquals(opprettetOverstyrBeregning?.sakId, overstyrBeregning?.sakId)
        assertEquals(opprettetOverstyrBeregning?.beskrivelse, overstyrBeregning?.beskrivelse)
    }

    @Test
    fun `skal lagre og hente en overstyr beregning og deretter kunne slette overstyrt beregning`() {
        val sakId = sakId1
        val opprettetOverstyrBeregning =
            beregningRepository.opprettOverstyrBeregning(
                OverstyrBeregning(sakId, "Test", Tidspunkt.now(), kategori = OverstyrtBeregningKategori.UKJENT_KATEGORI),
            )

        val overstyrBeregning = beregningRepository.hentOverstyrBeregning(sakId)

        assertNotNull(overstyrBeregning)

        assertEquals(opprettetOverstyrBeregning?.sakId, overstyrBeregning?.sakId)
        assertEquals(opprettetOverstyrBeregning?.beskrivelse, overstyrBeregning?.beskrivelse)

        beregningRepository.deaktiverOverstyrtBeregning(sakId)
        val overstyrBeregningSlettet = beregningRepository.hentOverstyrBeregning(sakId)
        assertNull(overstyrBeregningSlettet)
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
                .Metadata(sakId1, 1),
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
                    regelverk = Regelverk.BP_REGELVERK_FOM_2024,
                    kilde = Grunnlagsopplysning.RegelKilde("regelid", Tidspunkt.now(), "1"),
                    kunEnJuridiskForelder = true,
                ),
            ),
        overstyrBeregning = overstyrBeregning,
    )

    private fun Beregning?.fjernTilfeldigPeriodeIds() = this?.copy(beregningsperioder = this.beregningsperioder.map { it.copy(id = null) })
}
