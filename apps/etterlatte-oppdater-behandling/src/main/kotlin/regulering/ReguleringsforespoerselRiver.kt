package no.nav.etterlatte.regulering

import com.fasterxml.jackson.databind.node.MissingNode
import no.nav.etterlatte.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.ANTALL
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.KJOERING
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents.SPESIFIKKE_SAKER
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.SAK_TYPE
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.etterlatte.rapidsandrivers.saker
import no.nav.etterlatte.rapidsandrivers.tilbakestilteBehandlinger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.math.max
import kotlin.math.min

internal class ReguleringsforespoerselRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(ReguleringsforespoerselRiver::class.java)

    init {
        initialiserRiver(rapidsConnection, ReguleringHendelseType.REGULERING_STARTA) {
            validate { it.requireKey(DATO_KEY) }
            validate { it.requireKey(KJOERING) }
            validate { it.requireKey(ANTALL) }
            validate { it.requireKey(SPESIFIKKE_SAKER) }
            validate { it.interestedIn(SAK_TYPE) }
        }
    }

    override fun kontekst() = Kontekst.REGULERING

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info("Leser reguleringsforespørsel for dato ${packet.dato}")

        if (!featureToggleService.isEnabled(ReguleringFeatureToggle.START_REGULERING, false)) {
            logger.info("Regulering er deaktivert ved funksjonsbryter. Avbryter reguleringsforespørsel.")
            return
        }

        val kjoering = packet[KJOERING].asText()
        val antall = packet[ANTALL].asInt()
        val spesifikkeSaker = packet.saker
        val sakType = packet.optionalSakType()

        val maksBatchstoerrelse = MAKS_BATCHSTOERRELSE
        var tatt = 0

        while (tatt < antall) {
            val antallIDenneRunden = max(0, min(maksBatchstoerrelse, antall - tatt))
            logger.info("Starter å ta $antallIDenneRunden av totalt $antall saker")
            val sakerTilOmregning =
                behandlingService.hentAlleSaker(kjoering, antallIDenneRunden, spesifikkeSaker, sakerViIkkeRegulererAutomatiskNaa, sakType)
            logger.info("Henta ${sakerTilOmregning.saker.size} saker")

            if (sakerTilOmregning.saker.isEmpty()) {
                logger.debug("Ingen saker i denne runden. Returnerer")
                break
            }

            val tilbakemigrerte =
                behandlingService.migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(sakerTilOmregning)
                    .also { sakIdListe ->
                        logger.info(
                            "Tilbakeført ${sakIdListe.ider.size} behandlinger til trygdetid oppdatert:\n" +
                                sakIdListe.ider.joinToString("\n") { "Sak ${it.sakId} - ${it.behandlingId}" },
                        )
                    }

            sakerTilOmregning.saker.forEach {
                logger.debug("Lagrer kjøring starta for sak ${it.id}")
                behandlingService.lagreKjoering(it.id, KjoeringStatus.STARTA, kjoering)
                logger.debug("Ferdig lagra kjøring starta for sak ${it.id}")
                packet.setEventNameForHendelseType(ReguleringHendelseType.SAK_FUNNET)
                packet.tilbakestilteBehandlinger = tilbakemigrerte.behandlingerForSak(it.id)
                packet.sakId = it.id
                logger.debug("Sender til omregning for sak ${it.id}")
                context.publish(packet.toJson())
            }
            tatt += sakerTilOmregning.saker.size
            logger.info("Ferdig med $tatt av totalt $antall saker")
            if (sakerTilOmregning.saker.size < maksBatchstoerrelse) {
                break
            }
            val venteperiode = Duration.ofSeconds(5)
            logger.info("Venter $venteperiode før neste runde.")
            Thread.sleep(venteperiode)
        }
    }

    private fun JsonMessage.optionalSakType(): SakType? {
        return when (val node = this[SAK_TYPE]) {
            is MissingNode -> null
            else -> SakType.valueOf(node.asText())
        }
    }

    companion object {
        const val MAKS_BATCHSTOERRELSE = 100
    }
}

enum class ReguleringFeatureToggle(private val key: String) : FeatureToggle {
    START_REGULERING("start-regulering"),
    ;

    override fun key() = key
}

private val sakerViIkkeRegulererAutomatiskNaa =
    listOf<Long>(
        16850, // Til samordning
        17003, // Til samordning
        16616, // Til samordning
        3482, // Revurdering med overstyrt beregning åpen behandling
        6323, // Revurdering med overstyrt beregning åpen behandling
        11606, // Revurdering med overstyrt beregning åpen behandling
        11848, // Revurdering med overstyrt beregning åpen behandling
        6402, // EKSPORT: Feilmelding: Virkningstidspunkt kan ikke være etter opphør
        8883, // Feil i grunnlag.
        // https://logs.adeo.no/app/discover#/doc/96e648c0-980a-11e9-830a-e17bbd64b4db/.ds-navlogs-2024.05.28-000011?id=aiDgv48BZiQzzTM0q6EU
        9455, // Ulik versjon av grunnlag brukt i trygdetid og behandling
        // Herifra og ut: overstyrte beregninger. Tas seinare i separat køyring.
        11510,
        11580,
        16774,
        11931,
        14691,
        11963,
        15323,
        12269,
        12099,
        11074,
        11869,
        12280,
        11087,
        15409,
        11915,
        12119,
        12106,
        11444,
        15292,
        12082,
        11571,
        11556,
        13125,
        11446,
        11965,
        12295,
        11866,
        11519,
        12276,
        11584,
        11470,
        15256,
        11072,
        12381,
        13948,
        15716,
        11935,
        12374,
        12371,
        8919,
        15523,
        12293,
        13894,
        13730,
        12089,
        11949,
        14488,
        11891,
        15157,
        11604,
        12080,
        15136,
        11508,
        11449,
        11464,
        11908,
        14181,
        12081,
        13427,
        11559,
        11489,
        11488,
        13139,
        8941,
        11586,
        11468,
        15278,
        11623,
        12402,
        13868,
        11916,
        10592,
        11867,
        11954,
        11463,
        15449,
        12455,
        3206,
        11627,
        11381,
        11994,
        12277,
        11917,
        12302,
        12279,
        11450,
        11955,
        8924,
        12287,
        11618,
        11598,
        11075,
        12291,
        11962,
        11889,
        11624,
        11499,
        16221,
        12469,
        13105,
        13849,
        11970,
        14517,
        13515,
        11504,
        11920,
        15149,
        11924,
        11076,
        11972,
        11607,
        11496,
        11089,
        13770,
        11911,
        13759,
        12117,
        12336,
        15185,
        11469,
        11594,
        11252,
        11918,
        11958,
        3482,
        11459,
        12121,
        11950,
        11465,
        11533,
        12282,
        12091,
        15222,
        12288,
        6323,
        11921,
        11960,
        11497,
        14888,
        12273,
        11602,
        11070,
        13846,
        11625,
        12255,
        11453,
        12294,
        11590,
        11582,
        11907,
        12335,
        11558,
        11956,
        14543,
        11971,
        11892,
        11930,
        12111,
        11077,
        11505,
        11085,
        12472,
        15221,
        11653,
        13341,
        11652,
        11848,
        12105,
        11587,
        12114,
        11605,
        12060,
        11452,
        12100,
        12476,
        11890,
        12329,
        12124,
        12281,
        11936,
        12097,
        11945,
        11959,
        11595,
        8940,
        15403,
        12092,
        11600,
        8964,
        16067,
        11451,
        13462,
        12088,
        11507,
        15097,
        13513,
        11934,
        11509,
        11919,
        11588,
        11471,
        12270,
        14000,
        14128,
        12385,
        11572,
        11574,
        12094,
        11599,
        12301,
        14453,
        11948,
        11517,
        11461,
        11933,
        12404,
        11910,
        11906,
        12096,
        11964,
        12113,
        14220,
        14600,
        15538,
        12093,
        11498,
        11082,
        11606,
        11923,
        15279,
        11651,
        12118,
        11868,
        12484,
        11973,
        13912,
        11581,
        12101,
        11081,
        11577,
        12454,
        11932,
        11871,
        11655,
        11865,
        11476,
        12452,
        13941,
        11467,
        11974,
        11953,
        12272,
        11518,
        11414,
        11946,
        11628,
        11961,
        12090,
        11585,
        11596,
        14908,
        16061,
        14929,
        11593,
        11466,
        12125,
        13942,
        11957,
        11597,
        11513,
        3257,
        11591,
        13514,
        11947,
        11925,
        12449,
        11601,
        11503,
        12328,
        8926,
        11967,
        15440,
        12112,
        11445,
        11455,
        12095,
        15565,
        12120,
        12122,
        14939,
        10154,
        13910,
        11952,
        11073,
        8956,
        12098,
        11491,
        12123,
        11472,
        12115,
        11090,
        15919,
        14854,
        11909,
        15582,
        11515,
        12284,
        11951,
        2912,
        11495,
        11475,
        11583,
        12268,
        11500,
        13336,
        12373,
        15580,
        8925,
        15558,
        16216,
        12299,
        11490,
        11870,
        8934,
        11454,
        11071,
        14200,
        12275,
        11069,
        11492,
        11494,
        16052,
        11473,
        8962,
        12109,
        15549,
        11575,
    )
