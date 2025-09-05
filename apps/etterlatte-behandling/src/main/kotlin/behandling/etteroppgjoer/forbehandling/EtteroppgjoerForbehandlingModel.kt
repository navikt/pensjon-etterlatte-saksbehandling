package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import no.nav.etterlatte.behandling.etteroppgjoer.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.SummerteInntekterAOrdningen
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingDto
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

data class EtteroppgjoerForbehandling(
    val id: UUID,
    val hendelseId: UUID,
    val opprettet: Tidspunkt,
    val status: EtteroppgjoerForbehandlingStatus,
    val sak: Sak,
    val aar: Int,
    val innvilgetPeriode: Periode,
    val brevId: Long?,
    val varselbrevSendt: LocalDate?,
    val sisteIverksatteBehandlingId: UUID,
    val harMottattNyInformasjon: JaNei?,
    val endringErTilUgunstForBruker: JaNei?,
    val beskrivelseAvUgunst: String?,
    // hvis vi oppretter en kopi av forbehandling for å bruke i en revurdering
    val kopiertFra: UUID? = null,
) {
    companion object {
        fun opprett(
            sak: Sak,
            innvilgetPeriode: Periode,
            sisteIverksatteBehandling: UUID,
        ) = EtteroppgjoerForbehandling(
            id = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            sak = sak,
            status = EtteroppgjoerForbehandlingStatus.OPPRETTET,
            aar = innvilgetPeriode.fom.year,
            innvilgetPeriode = innvilgetPeriode,
            opprettet = Tidspunkt.now(),
            brevId = null,
            kopiertFra = null,
            sisteIverksatteBehandlingId = sisteIverksatteBehandling,
            harMottattNyInformasjon = null,
            endringErTilUgunstForBruker = null,
            beskrivelseAvUgunst = null,
            varselbrevSendt = null,
        )
    }

    fun tilBeregnet(): EtteroppgjoerForbehandling {
        if (status in listOf(EtteroppgjoerForbehandlingStatus.OPPRETTET, EtteroppgjoerForbehandlingStatus.BEREGNET)) {
            return copy(status = EtteroppgjoerForbehandlingStatus.BEREGNET)
        } else {
            throw InternfeilException("Kunne ikke endre status fra $status til ${EtteroppgjoerForbehandlingStatus.BEREGNET}")
        }
    }

    fun tilFerdigstilt(): EtteroppgjoerForbehandling {
        if (status == EtteroppgjoerForbehandlingStatus.BEREGNET) {
            return copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)
        } else {
            throw InternfeilException("Kunne ikke endre status fra $status til ${EtteroppgjoerForbehandlingStatus.FERDIGSTILT}")
        }
    }

    fun tilAvbrutt(): EtteroppgjoerForbehandling {
        if (kanAvbrytes()) {
            return copy(status = EtteroppgjoerForbehandlingStatus.AVBRUTT)
        } else {
            throw InternfeilException("Kunne ikke endre status fra $status til ${EtteroppgjoerForbehandlingStatus.AVBRUTT}")
        }
    }

    fun kanAvbrytes() = status !in listOf(EtteroppgjoerForbehandlingStatus.FERDIGSTILT, EtteroppgjoerForbehandlingStatus.AVBRUTT)

    fun medBrev(opprettetBrev: Brev): EtteroppgjoerForbehandling = this.copy(brevId = opprettetBrev.id)

    fun erUnderBehandling() =
        status in
            listOf(
                EtteroppgjoerForbehandlingStatus.OPPRETTET,
                EtteroppgjoerForbehandlingStatus.BEREGNET,
            )

    fun erFerdigstilt() =
        status in
            listOf(
                EtteroppgjoerForbehandlingStatus.FERDIGSTILT,
            )

    fun tilDto(): EtteroppgjoerForbehandlingDto =
        EtteroppgjoerForbehandlingDto(
            id = id,
            hendelseId = hendelseId,
            opprettet = opprettet,
            status = status,
            sak = sak,
            aar = aar,
            innvilgetPeriode = innvilgetPeriode,
            brevId = brevId,
            kopiertFra = kopiertFra,
            sisteIverksatteBehandlingId = sisteIverksatteBehandlingId,
            harMottattNyInformasjon = harMottattNyInformasjon,
            endringErTilUgunstForBruker = endringErTilUgunstForBruker,
            beskrivelseAvUgunst = beskrivelseAvUgunst,
            varselbrevSendt = varselbrevSendt,
        )
}

data class DetaljertForbehandlingDto(
    val behandling: EtteroppgjoerForbehandling,
    val opplysninger: EtteroppgjoerOpplysninger,
    val faktiskInntekt: FaktiskInntektDto?,
    val beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto?,
)

data class EtteroppgjoerOpplysninger(
    val skatt: PensjonsgivendeInntektFraSkatt,
    val ainntekt: AInntekt,
    val summerteInntekter: SummerteInntekterAOrdningen?,
    val tidligereAvkorting: AvkortingDto,
)
