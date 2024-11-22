package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.YearMonth

/*
* Inntektsjobb for 2025 innførte en feil i alle behandlinger som ble gjort automatisk som kompanseres for her.
* Alle behandlinger skal inneholde alle tidligere årsoppgjør men de automatiske behandlingene i jobben består kun av 2025.
* Det medfører at revurdering tilbake til 2024 mangler inntekten for 2024 når forrige behandling/avkorting kopieres.
*
* For å reparere dette gjøres det ekstra håndtering for å flette forrige avkorting med forrige avkorting i 2024.
* Denne må kjøre helt til alle saker har lagd en ny revurdering.
*
* Når alle saker har en ny revurdering hver (f.eks ved regulering eller etteroppgjør)
* kan dette fjernes og kun kopiere forrige avkorting.
*
*/
class AvkortingReparerAarsoppgjoeret(
    val avkortingRepository: AvkortingRepository,
) {
    fun hentSisteAvkortingMedReparertAarsoppgjoer(
        forrigeAvkorting: Avkorting,
        virkningstidspunkt: YearMonth,
        sakId: SakId,
        alleVedtak: List<VedtakSammendragDto>,
    ): Avkorting {
        val alleAarMedAarsoppgjoer = avkortingRepository.hentAlleAarsoppgjoer(sakId).map { it.aar }.distinct()
        val manglerAar = alleAarMedAarsoppgjoer != forrigeAvkorting.aarsoppgjoer.map { it.aar }

        if (manglerAar) {
            val sisteAarsoppgjoer = forrigeAvkorting.aarsoppgjoer.maxBy { it.aar }
            if (sisteAarsoppgjoer.aar < virkningstidspunkt.year) {
                // TODO to unittester
                if (virkningstidspunkt != YearMonth.of(virkningstidspunkt.year, 1)) {
                    throw FoersteRevurderingSenereEnnJanuar()
                }
                return forrigeAvkorting
            }

            // TODO en unittester - mock liste med vedtak som har opphør i mellom
            val sisteBehandlingSammeAar = alleVedtak.sisteLoependeVedtakForAar(virkningstidspunkt.year).behandlingId

            val forrigeAvkortingSammeAar =
                avkortingRepository.hentAvkorting(sisteBehandlingSammeAar)
                    ?: throw TidligereAvkortingFinnesIkkeException(sisteBehandlingSammeAar)

            // TODO to unittester
            val forrigeAvkortingHarAarsoppgjoerForVirk =
                forrigeAvkortingSammeAar.aarsoppgjoer.map { it.aar }.contains(virkningstidspunkt.year)
            return forrigeAvkortingSammeAar.copy(
                aarsoppgjoer =
                    if (forrigeAvkortingHarAarsoppgjoerForVirk) {
                        val nyttAarsoppgjoer =
                            forrigeAvkorting.aarsoppgjoer.single {
                                it.aar == virkningstidspunkt.year
                            }
                        forrigeAvkortingSammeAar.aarsoppgjoer.erstattEtAarsoppgjoer(nyttAarsoppgjoer)
                    } else {
                        forrigeAvkortingSammeAar.aarsoppgjoer + forrigeAvkorting.aarsoppgjoer
                    },
            )
        } else {
            // TODO en unittester
            return forrigeAvkorting
        }
    }
}

fun List<Aarsoppgjoer>.erstattEtAarsoppgjoer(nyttAarsoppgjoer: Aarsoppgjoer) =
    map {
        when (it.aar) {
            nyttAarsoppgjoer.aar -> nyttAarsoppgjoer
            else -> it
        }
    }

fun List<VedtakSammendragDto>.sisteLoependeVedtakForAar(aar: Int) =
    filter {
        val vedtakAar = it.virkningstidspunkt?.year ?: throw InternfeilException("Vedtak mangler virk")
        it.vedtakType != VedtakType.OPPHOER && (vedtakAar) != aar
    }.maxBy {
        it.datoAttestert ?: throw InternfeilException("Iverksatt vedtak mangler dato attestert")
    }
