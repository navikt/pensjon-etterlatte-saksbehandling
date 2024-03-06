package no.nav.etterlatte.migrering.vent

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.DATO_KEY
import no.nav.etterlatte.rapidsandrivers.OPPGAVE_ID_FLERE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_KJORING_VARIANT
import no.nav.etterlatte.rapidsandrivers.migrering.OPPGAVEKILDE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.OPPGAVETYPE_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Ventehendelser
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.Duration

class StartAaTaAvVent(
    private val ventRepository: VentRepository,
    private val rapidsConnection: RapidsConnection,
    featureToggleService: FeatureToggleService,
    sleep: (millis: Duration) -> Unit = { Thread.sleep(it) },
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        if (featureToggleService.isEnabled(VentFeatureToggle.TaAvVent, false)) {
            sleep(Duration.ofMinutes(1))
            taAvVent()
        }
    }

    internal fun taAvVent() {
        val avVent: SkalAvVentDTO? = ventRepository.hentSakerSomSkalAvVent()
        if (avVent == null) {
            logger.info("Ingenting å ta av vent. Avbryter")
            return
        }
        val melding = lagMelding(avVent)
        rapidsConnection.publish(melding)
        ventRepository.settSakerAvVent(avVent)
    }

    private fun lagMelding(avVent: SkalAvVentDTO) =
        JsonMessage.newMessage(
            mapOf(
                Ventehendelser.TA_AV_VENT.lagParMedEventNameKey(),
                OPPGAVE_ID_FLERE_KEY to avVent.oppgaver,
                DATO_KEY to avVent.dato,
                OPPGAVEKILDE_KEY to OppgaveKilde.GJENOPPRETTING,
                OPPGAVETYPE_KEY to OppgaveType.FOERSTEGANGSBEHANDLING,
                MIGRERING_KJORING_VARIANT to avVent.kjoringVariant,
            ),
        ).toJson()
}

enum class VentFeatureToggle(private val key: String) : FeatureToggle {
    TaAvVent("ta-av-vent"),
    ;

    override fun key() = key
}
