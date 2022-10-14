package barnepensjon.vilkaar

import GrunnlagTestData
import barnepensjon.vilkaarFormaalForYtelsen
import grunnlag.kilde
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class FormaalTest {
    private val doedsdato = LocalDate.of(2012, 10, 5)
    private val soekerILive = GrunnlagTestData().hentOpplysningsgrunnlag().soeker
    private val soekerDoed = GrunnlagTestData(
        opplysningsmapSoekerOverrides = mapOf(
            Opplysningstype.DOEDSDATO to Opplysning.Konstant(UUID.randomUUID(), kilde, doedsdato.toJsonNode())
        )
    ).hentOpplysningsgrunnlag().soeker

    @Test
    fun `soeker som er i live paa virkningstidspunkt oppfyller vilkaar`() {
        val virkningstidspunkt = YearMonth.from(doedsdato).plusMonths(1).atDay(1)
        val soekerILivePaaVirkningstidspunkt = soekerDoed + mapOf(
            Opplysningstype.DOEDSDATO to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                doedsdato.plusMonths(2).toJsonNode()
            )
        )
        val vurderingMedDoedsdato = vilkaarFormaalForYtelsen(
            soekerILivePaaVirkningstidspunkt,
            virkningstidspunkt
        )
        val vurderingUtenDoedsdato = vilkaarFormaalForYtelsen(
            soekerILive,
            virkningstidspunkt
        )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurderingMedDoedsdato.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurderingUtenDoedsdato.resultat)
    }

    @Test
    fun `ukjent data for soeker gir maa avklares for vilkaar`() {
        val vurderingUkjentVirk = vilkaarFormaalForYtelsen(soekerILive, null)
        val vurderingUkjentSoeker = vilkaarFormaalForYtelsen(
            null,
            YearMonth.from(doedsdato).plusMonths(1).atDay(1)
        )
        val vurderingUkjentBegge = vilkaarFormaalForYtelsen(null, null)

        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vurderingUkjentBegge.resultat
        )
        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vurderingUkjentVirk.resultat
        )
        Assertions.assertEquals(
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING,
            vurderingUkjentSoeker.resultat
        )
    }

    @Test
    fun `soeker med doedsdato tidligere enn virkningsdato oppfyller ikke vurdering`() {
        val vurdering = vilkaarFormaalForYtelsen(
            soekerDoed,
            virkningstidspunkt = YearMonth.from(doedsdato).plusMonths(1).atDay(1)
        )

        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, vurdering.resultat)
    }
}