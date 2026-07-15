package no.nav.etterlatte.brukerdialog.soeknad

import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.brukerdialog.soeknad.client.BehandlingClient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.event.SoeknadInnsendtHendelseType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.InnsendtSoeknad
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.LoggerFactory

data class SoeknadSkyggeRequest(
    val soeknadId: String,
    val sakType: SakType,
    val fnrSoeker: String,
)

enum class ProsesseringSkyggeToggle(
    private val toggle: String,
) : FeatureToggle {
    SKYGGE_SOEKNADMOTTAK("prosessering-soeknad-skygge"),
    ;

    override fun key(): String = toggle
}

/**
 * Skyggekjøring (PoC Fase 4): lytter på det samme søknad-eventet som [NySoeknadRiver],
 * men **muterer ingenting** — ingen `precondition` som konsumerer eventet og ingen
 * `publish`. Den kaller behandling-REST for å legge en prosessering-task i kø, slik
 * at søknaden også håndteres «på prosesserings-måten» parallelt med dagens flyt.
 *
 * Hele riveren er gated bak [ProsesseringSkyggeToggle.SKYGGE_SOEKNADMOTTAK] (default av),
 * så den er mørk til vi flipper den i dev.
 */
internal class SoeknadSkyggeRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingKlient: BehandlingClient,
    private val featureToggleService: FeatureToggleService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT) {
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoKey) }
            validate { it.requireKey(SoeknadInnsendt.lagretSoeknadIdKey) }
            validate { it.requireKey(SoeknadInnsendt.fnrSoekerKey) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        if (!featureToggleService.isEnabled(ProsesseringSkyggeToggle.SKYGGE_SOEKNADMOTTAK, false)) {
            return
        }

        val soeknadId = packet[SoeknadInnsendt.lagretSoeknadIdKey].longValue().toString()
        val soeknad = objectMapper.treeToValue<InnsendtSoeknad>(packet[SoeknadInnsendt.skjemaInfoKey])
        val sakType =
            when (soeknad.type) {
                SoeknadType.BARNEPENSJON -> SakType.BARNEPENSJON
                SoeknadType.OMSTILLINGSSTOENAD -> SakType.OMSTILLINGSSTOENAD
            }

        behandlingKlient.opprettSoeknadSkyggeTask(
            SoeknadSkyggeRequest(
                soeknadId = soeknadId,
                sakType = sakType,
                fnrSoeker = packet[SoeknadInnsendt.fnrSoekerKey].textValue(),
            ),
        )
        logger.info("Skyggekjøring: la søknad $soeknadId ($sakType) i prosessering-kø")
    }
}
