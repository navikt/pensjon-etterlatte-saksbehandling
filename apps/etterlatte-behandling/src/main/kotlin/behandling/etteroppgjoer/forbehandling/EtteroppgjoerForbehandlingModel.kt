package no.nav.etterlatte.behandling.etteroppgjoer.forbehandling

import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.SummerteInntekterAOrdningen
import no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt.SummertePensjonsgivendeInntekter
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.AarsakTilAvbryteForbehandling
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingDto
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate
import java.util.UUID

data class EtteroppgjoerForbehandling(
    val id: UUID,
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
    val aarsakTilAvbrytelse: AarsakTilAvbryteForbehandling? = null,
    val aarsakTilAvbrytelseBeskrivelse: String? = null,
    val kopiertFra: UUID? = null,
    val etteroppgjoerResultatType: EtteroppgjoerResultatType? = null,
    val harVedtakAvTypeOpphoer: Boolean? = null,
    val opphoerSkyldesDoedsfall: JaNei?,
    val opphoerSkyldesDoedsfallIEtteroppgjoersaar: JaNei?,
) {
    companion object {
        fun opprett(
            sak: Sak,
            innvilgetPeriode: Periode,
            sisteIverksatteBehandling: UUID,
            harVedtakAvTypeOpphoer: Boolean? = null,
        ) = EtteroppgjoerForbehandling(
            id = UUID.randomUUID(),
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
            harVedtakAvTypeOpphoer = harVedtakAvTypeOpphoer,
            opphoerSkyldesDoedsfall = null,
            opphoerSkyldesDoedsfallIEtteroppgjoersaar = null,
        )
    }

    fun skyldesOpphoerDoedsfallIEtteroppgjoersaar() = opphoerSkyldesDoedsfallIEtteroppgjoersaar == JaNei.JA

    fun tilBeregnet(beregnetEtteroppgjoerResultatDto: BeregnetEtteroppgjoerResultatDto): EtteroppgjoerForbehandling {
        if (!erUnderBehandling()) {
            throw EtteroppgjoerForbehandlingStatusException(this, EtteroppgjoerForbehandlingStatus.BEREGNET)
        }
        return copy(
            status = EtteroppgjoerForbehandlingStatus.BEREGNET,
            etteroppgjoerResultatType = beregnetEtteroppgjoerResultatDto.resultatType,
            brevId =
                this.brevId?.takeIf {
                    beregnetEtteroppgjoerResultatDto.resultatType !=
                        EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING
                },
        )
    }

    fun tilFerdigstilt(): EtteroppgjoerForbehandling {
        val kanFerdigstille =
            if (skyldesOpphoerDoedsfallIEtteroppgjoersaar()) {
                erUnderBehandling()
            } else {
                status == EtteroppgjoerForbehandlingStatus.BEREGNET
            }

        if (!kanFerdigstille) {
            throw EtteroppgjoerForbehandlingStatusException(this, EtteroppgjoerForbehandlingStatus.FERDIGSTILT)
        }

        // TODO: validere her at vi kan ferdigstille uten brev?

        return copy(status = EtteroppgjoerForbehandlingStatus.FERDIGSTILT)
    }

    fun tilAvbrutt(
        aarsak: AarsakTilAvbryteForbehandling,
        kommentar: String?,
    ): EtteroppgjoerForbehandling {
        if (!erRedigerbar()) {
            throw EtteroppgjoerForbehandlingStatusException(this, EtteroppgjoerForbehandlingStatus.AVBRUTT)
        }
        return copy(
            status = EtteroppgjoerForbehandlingStatus.AVBRUTT,
            aarsakTilAvbrytelse = aarsak,
            aarsakTilAvbrytelseBeskrivelse = kommentar,
        )
    }

    fun tilDto(): EtteroppgjoerForbehandlingDto =
        EtteroppgjoerForbehandlingDto(
            id = id,
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

    fun medBrev(opprettetBrev: Brev): EtteroppgjoerForbehandling = this.copy(brevId = opprettetBrev.id)

    fun medVarselbrevSendt(dato: LocalDate): EtteroppgjoerForbehandling {
        if (varselbrevSendt != null) {
            throw InternfeilException("Forbehandling har allerede varselbrev sendt tidspunkt=$varselbrevSendt")
        }
        return this.copy(varselbrevSendt = dato)
    }

    fun erUnderBehandling() =
        when (status) {
            EtteroppgjoerForbehandlingStatus.OPPRETTET,
            EtteroppgjoerForbehandlingStatus.BEREGNET,
            -> true
            else -> false
        }

    fun erFerdigstilt() = status == EtteroppgjoerForbehandlingStatus.FERDIGSTILT

    fun erRedigerbar() = erUnderBehandling() && !erFerdigstilt()

    // hvis vi oppretter en kopi av forbehandling for å bruke i en revurdering
    fun erRevurdering() = kopiertFra != null

    fun oppdaterBrukerHarSvart(
        harMottattNyInformasjon: JaNei?,
        endringErTilUgunstForBruker: JaNei?,
        beskrivelseAvUgunst: String?,
    ): EtteroppgjoerForbehandling =
        copy(
            harMottattNyInformasjon = harMottattNyInformasjon,
            endringErTilUgunstForBruker = endringErTilUgunstForBruker?.takeIf { harMottattNyInformasjon == JaNei.JA },
            beskrivelseAvUgunst =
                beskrivelseAvUgunst?.takeIf {
                    harMottattNyInformasjon == JaNei.JA &&
                        endringErTilUgunstForBruker == JaNei.JA
                },
        )

    fun oppdaterOmOpphoerSkyldesDoedsfall(
        opphoerSkyldesDoedsfall: JaNei,
        opphoerSkyldesDoedsfallIEtteroppgjoersaar: JaNei?,
    ): EtteroppgjoerForbehandling =
        copy(
            opphoerSkyldesDoedsfall = opphoerSkyldesDoedsfall,
            opphoerSkyldesDoedsfallIEtteroppgjoersaar =
                opphoerSkyldesDoedsfallIEtteroppgjoersaar?.takeIf {
                    opphoerSkyldesDoedsfall == JaNei.JA
                },
        )
}

class EtteroppgjoerForbehandlingStatusException(
    forbehandling: EtteroppgjoerForbehandling,
    statusNy: EtteroppgjoerForbehandlingStatus,
) : InternfeilException(
        "Kunne ikke endre status fra ${forbehandling.status} til $statusNy, for behandling med id=${forbehandling.id}",
    )

data class DetaljertForbehandlingDto(
    val forbehandling: EtteroppgjoerForbehandling,
    val opplysninger: EtteroppgjoerOpplysninger,
    val faktiskInntekt: FaktiskInntektDto?,
    val beregnetEtteroppgjoerResultat: BeregnetEtteroppgjoerResultatDto?,
)

data class EtteroppgjoerOpplysninger(
    val skatt: SummertePensjonsgivendeInntekter,
    val summerteInntekter: SummerteInntekterAOrdningen?,
    val tidligereAvkorting: AvkortingDto?,
)
