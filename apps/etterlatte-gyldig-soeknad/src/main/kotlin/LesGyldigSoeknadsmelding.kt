import model.GyldigSoeknadService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class LesGyldigSoeknadsmelding(
    rapidsConnection: RapidsConnection,
    private val gyldigSoeknad: GyldigSoeknadService,
    private val pdl: Pdl,
    private val behandling: Behandling,
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesGyldigSoeknadsmelding::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "ey_fordelt") }
            validate { it.demandValue("@soeknad_fordelt", true) }
            validate { it.interestedIn("@correlation_id") }
            validate { it.requireKey("@skjema_info") }
            validate { it.demandValue("@skjema_info.type", "BARNEPENSJON") }
            validate { it.demandValue("@skjema_info.versjon", "2") }
            validate { it.requireKey("@lagret_soeknad_id") }
            validate { it.requireKey("@fnr_soeker") } //TODO: sjekk at dette er riktig verdi
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            logger.info("Gyldighetsvurdering av mottat søknad fra fordeler starter")

            try {
                val personGalleri = gyldigSoeknad.hentPersongalleriFraSoeknad(packet["@skjema_info"])
                val familieRelasjonPdl = gyldigSoeknad.hentSoekerFraPdl(personGalleri.soeker, pdl)
                val gyldighetsVurdering = gyldigSoeknad.vurderGyldighet(personGalleri, familieRelasjonPdl)
                val erGyldigFramsatt = if (gyldighetsVurdering.resultat == VurderingsResultat.OPPFYLT) true else false
                logger.info("Gyldighetsvurdering I lesGyldigsoeknad: {}", gyldighetsVurdering)

                val sak = behandling.skaffSak(packet["@fnr_soeker"].asText(), packet["@skjema_info"]["type"].asText())
                val behandlingsid = behandling.initierBehandling(
                    sak, packet["@skjema_info"]["mottattDato"].asText(), personGalleri
                )
                behandling.lagreGyldighetsVurdering(behandlingsid, gyldighetsVurdering)

                packet["@sak_id"] = sak
                packet["@behandling_id"] = behandlingsid
                packet["@gyldig_innsender"] = erGyldigFramsatt

                context.publish(packet.toJson())
                logger.info("Vurdert gyldighet av søknad")
            } catch (e: Exception) {
                println("Gyldighetsvurdering feilet " + e)
            }
        }
}

interface Pdl {
    fun hentPdlModell(foedselsnummer: String, rolle: PersonRolle): Person
}

interface Behandling {
    fun initierBehandling(
        sak: Long,
        mottattDato: String,
        persongalleri: Persongalleri
    ): UUID

    fun skaffSak(person: String, saktype: String): Long
    fun lagreGyldighetsVurdering(behandlingsId: UUID, gyldighetsVurdering: GyldighetsResultat)
}

private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
