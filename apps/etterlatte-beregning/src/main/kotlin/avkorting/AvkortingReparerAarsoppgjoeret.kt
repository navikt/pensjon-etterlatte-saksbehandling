package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentSisteAvkortingMedReparertAarsoppgjoer(
        forrigeAvkorting: Avkorting,
        virkningstidspunkt: YearMonth,
        sakId: SakId,
        alleVedtak: List<VedtakSammendragDto>,
    ): Avkorting {
        if (YearMonth.now() > YearMonth.of(2025, 5)) {
            logger.warn("AvkortingReparerAarsoppgjoeret.kt har nå med sikkerhet blitt kjørt på alle saker etter regulering og kan fjernes!")
        }

        val alleAarMedAarsoppgjoer = avkortingRepository.hentAlleAarsoppgjoer(sakId).map { it.aar }.toSet()
        val alleAarNyAvkortng = forrigeAvkorting.aarsoppgjoer.map { it.aar }.toSet()
        val manglerAar = alleAarMedAarsoppgjoer != alleAarNyAvkortng

        if (manglerAar) {
            val sisteAarsoppgjoer = forrigeAvkorting.aarsoppgjoer.maxBy { it.aar }
            if (sisteAarsoppgjoer.aar < virkningstidspunkt.year) {
                if (virkningstidspunkt != YearMonth.of(virkningstidspunkt.year, 1)) {
                    throw FoersteRevurderingSenereEnnJanuar()
                }
                return forrigeAvkorting
            }

            val manglendeAar =
                when (alleAarNyAvkortng.contains(virkningstidspunkt.year)) {
                    true -> virkningstidspunkt.year.minus(1)
                    false -> virkningstidspunkt.year
                }

            val sisteBehandlingManglendeAar = alleVedtak.sisteLoependeVedtakForAar(manglendeAar).behandlingId
            val sisteAvkortingManglendeAar =
                avkortingRepository.hentAvkorting(sisteBehandlingManglendeAar)
                    ?: throw TidligereAvkortingFinnesIkkeException(sisteBehandlingManglendeAar)

            return sisteAvkortingManglendeAar.copy(
                aarsoppgjoer = sisteAvkortingManglendeAar.aarsoppgjoer + forrigeAvkorting.aarsoppgjoer,
            )
        } else {
            return forrigeAvkorting
        }
    }

    fun hentAvkortingForSistIverksattMedReparertAarsoppgjoer(
        sakId: SakId,
        alleVedtak: List<VedtakSammendragDto>,
        avkortingSistIverksatt: Avkorting,
    ): Avkorting {
        val alleAarMedAarsoppgjoer = avkortingRepository.hentAlleAarsoppgjoer(sakId).map { it.aar }.toSet()
        val alleAarNyAvkortng = avkortingSistIverksatt.aarsoppgjoer.map { it.aar }.toSet()
        val manglerAar = alleAarMedAarsoppgjoer != alleAarNyAvkortng

        if (manglerAar) {
            val alleManglendeAar = alleAarMedAarsoppgjoer - alleAarNyAvkortng
            val aarsoppgjoerManglende =
                alleManglendeAar.toSortedSet().map { manglendeAar ->
                    val behandlingId = alleVedtak.sisteLoependeVedtakForAar(manglendeAar).behandlingId
                    val avkorting =
                        avkortingRepository.hentAvkorting(behandlingId)
                            ?: throw InternfeilException("Kunne ikke hente avkorting som skal finnes for $behandlingId")
                    avkorting.aarsoppgjoer.single { manglendeAar == it.aar }
                }
            return avkortingSistIverksatt.copy(
                aarsoppgjoer = avkortingSistIverksatt.aarsoppgjoer + aarsoppgjoerManglende,
            )
        } else {
            return avkortingSistIverksatt
        }
    }
}

fun List<VedtakSammendragDto>.sisteLoependeVedtakForAar(aar: Int) =
    filter {
        val vedtakAar = it.virkningstidspunkt?.year ?: throw InternfeilException("Vedtak mangler virk")
        it.vedtakType != VedtakType.OPPHOER && vedtakAar == aar
    }.maxBy {
        it.datoAttestert ?: throw InternfeilException("Iverksatt vedtak mangler dato attestert")
    }
