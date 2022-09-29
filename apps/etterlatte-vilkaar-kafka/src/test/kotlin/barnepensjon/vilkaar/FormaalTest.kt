package barnepensjon.vilkaar

import GrunnlagTestData
import barnepensjon.vilkaarFormaalForYtelsen
import grunnlag.kilde
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class FormaalTest {
    private val dødsdato = LocalDate.of(2012, 10, 5)
    private val søkerILive = GrunnlagTestData().hentOpplysningsgrunnlag().søker
    private val søkerDød = GrunnlagTestData(
        opplysningsmapSøkerOverrides = mapOf(
            Opplysningstyper.DOEDSDATO to Opplysning.Konstant(UUID.randomUUID(), kilde, dødsdato.toJsonNode())
        )
    ).hentOpplysningsgrunnlag().søker

    @Test
    fun `søker som er i live på virkningstidspunkt oppfyller vilkaar`() {
        val virkningstidspunkt = YearMonth.from(dødsdato).plusMonths(1).atDay(1)
        val søkerILivePåVirkningstidspunkt = søkerDød + mapOf(
            Opplysningstyper.DOEDSDATO to Opplysning.Konstant(
                UUID.randomUUID(),
                kilde,
                dødsdato.plusMonths(2).toJsonNode()
            )
        )
        val vurderingMedDoedsdato = vilkaarFormaalForYtelsen(
            søkerILivePåVirkningstidspunkt,
            virkningstidspunkt
        )
        val vurderingUtenDoedsdato = vilkaarFormaalForYtelsen(
            søkerILive,
            virkningstidspunkt
        )

        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurderingMedDoedsdato.resultat)
        Assertions.assertEquals(VurderingsResultat.OPPFYLT, vurderingUtenDoedsdato.resultat)
    }

    @Test
    fun `ukjent data for søker gir må avklares for vilkår`() {
        val vurderingUkjentVirk = vilkaarFormaalForYtelsen(søkerILive, null)
        val vurderingUkjentSoeker = vilkaarFormaalForYtelsen(
            null,
            YearMonth.from(dødsdato).plusMonths(1).atDay(1)
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
    fun `søker med dødsdato tidligere enn virkningsdato oppfyller ikke vurdering`() {
        val vurdering = vilkaarFormaalForYtelsen(
            søkerDød,
            virkningstidspunkt = YearMonth.from(dødsdato).plusMonths(1).atDay(1)
        )

        Assertions.assertEquals(VurderingsResultat.IKKE_OPPFYLT, vurdering.resultat)
    }
}