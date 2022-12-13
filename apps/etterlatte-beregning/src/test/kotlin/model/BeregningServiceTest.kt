package model

import GrunnlagTestData
import grunnlag.kilde
import io.mockk.mockk
import no.nav.etterlatte.BeregningRepository
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.model.BeregningService
import no.nav.etterlatte.model.VilkaarsvurderingKlient
import no.nav.etterlatte.model.behandling.BehandlingKlientImpl
import no.nav.etterlatte.model.beregnSisteTom
import no.nav.etterlatte.model.grunnlag.GrunnlagKlientImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import vilkaarsvurdering.VilkaarsvurderingTestData
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID.randomUUID

internal class BeregningServiceTest {

    private val vilkaarsvurdering = VilkaarsvurderingTestData.oppfylt
    private val behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
    private val behandlingId = randomUUID()
    private val beregningRepository = mockk<BeregningRepository>()
    private val vilkaarsvurderingKlientImpl = mockk<VilkaarsvurderingKlient>()
    private val grunnlagKlientImpl = mockk<GrunnlagKlientImpl>()
    private val behandlingKlientImpl = mockk<BehandlingKlientImpl>()
    private val testData = GrunnlagTestData(
        opplysningsmapSoeskenOverrides = mapOf(
            Opplysningstype.FOEDSELSDATO to Opplysning.Konstant(
                randomUUID(),
                kilde,
                LocalDate.of(2003, 12, 12).toJsonNode()
            )
        )
    )
    private val opplysningsgrunnlag = testData.hentOpplysningsgrunnlag()

    private val beregningsperioder = BeregningService(
        beregningRepository,
        vilkaarsvurderingKlientImpl,
        grunnlagKlientImpl,
        behandlingKlientImpl
    ).lagBeregning(
        opplysningsgrunnlag,
        YearMonth.of(2021, 2),
        YearMonth.of(2021, 9),
        vilkaarsvurdering.resultat!!.utfall,
        behandlingType,
        behandlingId
    ).beregningsperioder

    @Test
    fun beregnResultat() {
        beregningsperioder[0].also {
            assertEquals(YearMonth.of(2021, 2), it.datoFOM)
            assertEquals(YearMonth.of(2021, 4), it.datoTOM)
        }
        beregningsperioder[1].also {
            assertEquals(YearMonth.of(2021, 5), it.datoFOM)
            assertEquals(YearMonth.of(2021, 8), it.datoTOM)
        }
        beregningsperioder[2].also {
            assertEquals(YearMonth.of(2021, 9), it.datoFOM)
            assertEquals(YearMonth.of(2021, 12), it.datoTOM)
        }
        beregningsperioder[3].also {
            assertEquals(YearMonth.of(2022, 1), it.datoFOM)
            assertEquals(null, it.datoTOM)
        }
    }

    @Test
    fun `ved revurdering og ikke oppfylte vilkaar skal beregningsresultat settes til kr 0`() {
        val virkFOM = YearMonth.of(2022, 5)
        val virkTOM = YearMonth.of(2022, 10)
        val resultat = BeregningService(
            beregningRepository,
            vilkaarsvurderingKlientImpl,
            grunnlagKlientImpl,
            behandlingKlientImpl
        ).lagBeregning(
            grunnlag = Grunnlag.empty(),
            virkFOM = virkFOM,
            virkTOM = virkTOM,
            vilkaarsvurderingUtfall = VilkaarsvurderingTestData.ikkeOppfylt.resultat!!.utfall,
            behandlingType = BehandlingType.REVURDERING,
            behandlingId
        )
        assertEquals(virkFOM, resultat.beregningsperioder.first().datoFOM)
        assertEquals(null, resultat.beregningsperioder.first().datoTOM)
        assertEquals(0, resultat.beregningsperioder.first().utbetaltBeloep)
    }

    @Test
    fun `ved manuelt opphoer skal virkFOM settes til foerste i maaneden etter doedsdato`() {
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val resultat = BeregningService(
            beregningRepository,
            vilkaarsvurderingKlientImpl,
            grunnlagKlientImpl,
            behandlingKlientImpl
        ).lagBeregning(
            grunnlag = grunnlag,
            virkFOM = mockk(),
            virkTOM = mockk(),
            vilkaarsvurderingUtfall = mockk(),
            behandlingType = BehandlingType.MANUELT_OPPHOER,
            behandlingId
        )

        assertEquals(1, resultat.beregningsperioder.size)
        assertEquals(YearMonth.of(2022, 9), resultat.beregningsperioder.first().datoFOM)
    }

    @Test
    fun `beregningsperiodene får riktig beloep`() {
        assertEquals(2534, beregningsperioder[0].utbetaltBeloep)
        assertEquals(2660, beregningsperioder[1].utbetaltBeloep)
        assertEquals(2660, beregningsperioder[2].utbetaltBeloep)
        assertEquals(2882, beregningsperioder[3].utbetaltBeloep)
    }

    @Nested
    inner class BeregnSisteTom {
        @Test
        fun `skal returnere foedselsdato om soeker blir 18 i loepet av perioden`() {
            val foedselsdato = LocalDate.of(2004, 3, 23)
            assertEquals(YearMonth.of(2022, 3), beregnSisteTom(foedselsdato, YearMonth.of(2022, 3)))

            val foedselsdato2 = LocalDate.of(2004, 2, 23)
            assertEquals(YearMonth.of(2022, 2), beregnSisteTom(foedselsdato2, YearMonth.of(2022, 3)))
        }

        @Test
        fun `skal returnere null om soeker er under 18 i hele perioden`() {
            val foedselsdato = LocalDate.of(2004, 4, 23)
            assertEquals(null, beregnSisteTom(foedselsdato, YearMonth.of(2022, 3)))
        }
    }
}