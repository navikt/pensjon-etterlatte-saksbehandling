package barnepensjon.vilkaar

import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
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

fun vilkaarKanBehandleSakenISystemet(
    opphoersgrunner: List<ManueltOpphoerAarsak>,
    fritekstGrunn: String?
): VurdertVilkaar {
    val grunnTilManueltOpphoerKriterie = kjenteGrunnerTilManueltOpphoer(opphoersgrunner, fritekstGrunn)

    return VurdertVilkaar(
        navn = Vilkaartyper.SAKEN_KAN_BEHANDLES_I_SYSTEMET,
        resultat = setVilkaarVurderingFraKriterier(listOf(grunnTilManueltOpphoerKriterie)),
        utfall = null,
        kriterier = listOf(grunnTilManueltOpphoerKriterie),
        vurdertDato = LocalDateTime.now()
    )
}

fun kjenteGrunnerTilManueltOpphoer(opphoersgrunner: List<ManueltOpphoerAarsak>, fritekstGrunn: String?): Kriterie {
    val harIngenGrunnerTilAnnulering = opphoersgrunner.isEmpty() && fritekstGrunn == null
    return Kriterie(
        navn = Kriterietyper.INGEN_GRUNN_TIL_ANNULERING,
        resultat = vurderOpplysning { harIngenGrunnerTilAnnulering },
        basertPaaOpplysninger = opphoersgrunner.map { it.tilKriteriegrunnlagManueltOpphoer() } + listOfNotNull(
            fritekstGrunn.let { it.tilKriterieGrunnlagManueltOpphoer() }
        )
    )
}

fun ManueltOpphoerAarsak.tilKriteriegrunnlagManueltOpphoer(): Kriteriegrunnlag<ManueltOpphoerAarsak> {
    return Kriteriegrunnlag(
        id = UUID.randomUUID(),
        kriterieOpplysningsType = KriterieOpplysningsType.SAKSBEHANDLER_RESULTAT,
        kilde = Grunnlagsopplysning.Saksbehandler(
            "TODO: Ta med identen på den som starter et manuelt opphør",
            Instant.now() // TODO("Sett denne på en mer rimelig måte?")
        ),
        opplysning = this
    )
}

fun String?.tilKriterieGrunnlagManueltOpphoer(): Kriteriegrunnlag<String>? {
    if (this.isNullOrEmpty()) {
        return null
    }
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