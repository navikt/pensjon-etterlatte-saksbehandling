package no.nav.etterlatte.avkorting

import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vedtak.erInnvilgetIAar
import org.slf4j.LoggerFactory

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
    private val logger = LoggerFactory.getLogger(AvkortingReparerAarsoppgjoeret::class.java)

    /** Forutsetter at avkortingen som skal hentes tilhører et iverksatt vedtak */
    fun hentAvkortingMedReparertAarsoppgjoer(
        avkorting: Avkorting,
        iverksatteVedtakPaaSak: List<VedtakSammendragDto>,
        innvilgedePerioder: List<InnvilgetPeriodeDto>,
    ): Avkorting {
        val paakrevdeAarsoppgjoer =
            avkortingRepository
                .hentAlleAarsoppgjoer(iverksatteVedtakPaaSak.map { it.behandlingId })
                .map { it.aar }
                .filter { aar -> innvilgedePerioder.erInnvilgetIAar(aar) } // Ytelsen har vært innvilget
                .toSet()
        val alleAarIAvkorting = avkorting.aarsoppgjoer.map { it.aar }.toSet()
        val manglendeAar = (paakrevdeAarsoppgjoer - alleAarIAvkorting).toSet()

        if (manglendeAar.isNotEmpty()) {
            logger.info(
                "Fant manglende årsoppgjør. Forrige årsoppgjør-ID: ${avkorting.aarsoppgjoer.firstOrNull()?.id}. " +
                    "Manglende år: " + manglendeAar,
            )
        }
        val aarsoppgjoerManglende =
            manglendeAar.map { aar ->
                aarsoppgjoerIVedtak(
                    iverksatteVedtakPaaSak.sisteLoependeVedtakForAar(aar),
                    aar,
                )
            }
        return Avkorting((avkorting.aarsoppgjoer + aarsoppgjoerManglende).sortedBy { it.aar })
    }

    private fun aarsoppgjoerIVedtak(
        vedtak: VedtakSammendragDto,
        aar: Int,
    ): Aarsoppgjoer {
        val avkortingen =
            avkortingRepository.hentAvkorting(vedtak.behandlingId)
                ?: throw TidligereAvkortingFinnesIkkeException(vedtak.behandlingId)
        return avkortingen.aarsoppgjoer.single { aar == it.aar }
    }
}

fun List<VedtakSammendragDto>.sisteLoependeVedtakForAar(aar: Int) =
    filter {
        val vedtakAar = it.virkningstidspunkt?.year ?: throw InternfeilException("Vedtak mangler virk")
        it.vedtakType != VedtakType.OPPHOER && vedtakAar == aar
    }.maxBy {
        it.datoAttestert ?: throw InternfeilException("Iverksatt vedtak mangler dato attestert")
    }
