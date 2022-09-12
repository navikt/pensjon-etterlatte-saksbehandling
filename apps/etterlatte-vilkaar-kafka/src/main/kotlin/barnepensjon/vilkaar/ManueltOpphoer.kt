package barnepensjon.vilkaar

import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

fun vilkaarKanBehandleSakenISystemet(opphoersgrunner: List<String>): VurdertVilkaar {
    val grunnTilManueltOpphoerKriterie = grunnTilManueltOpphoer(opphoersgrunner)

    return VurdertVilkaar(
        navn = Vilkaartyper.SAKEN_KAN_BEHANDLES_I_SYSTEMET,
        resultat = setVilkaarVurderingFraKriterier(listOf(grunnTilManueltOpphoerKriterie)),
        utfall = null,
        kriterier = listOf(grunnTilManueltOpphoerKriterie),
        vurdertDato = LocalDateTime.now()
    )
}

fun grunnTilManueltOpphoer(opphoersgrunner: List<String>): Kriterie {
    val harIngenGrunnerTilAnnulering = opphoersgrunner.isEmpty()

    return Kriterie(
        navn = Kriterietyper.INGEN_GRUNN_TIL_ANNULERING,
        resultat = vurderOpplysning { harIngenGrunnerTilAnnulering },
        basertPaaOpplysninger = opphoersgrunner.map { it.tilKriterieGrunnlagManueltOpphoer() }
    )
}

fun String.tilKriterieGrunnlagManueltOpphoer(): Kriteriegrunnlag<String> {
    return Kriteriegrunnlag(
        id = UUID.randomUUID(),
        kriterieOpplysningsType = KriterieOpplysningsType.SAKSBEHANDLER_RESULTAT,
        kilde = Grunnlagsopplysning.Saksbehandler(
            "TODO: Ta med identen på den som starter et manuelt opphør",
            Instant.now()
        ),
        opplysning = this
    )
}