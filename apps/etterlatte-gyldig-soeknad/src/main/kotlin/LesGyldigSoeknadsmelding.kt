import model.GyldigSoeknadService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
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
    private val behandling: Behandling,
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesGyldigSoeknadsmelding::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("FORDELER:FORDELT")
            correlationId()
            validate { it.demandValue("soeknadFordelt", true) }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("@fnr_soeker") } //TODO: sjekk at dette er riktig verdi
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            logger.info("Gyldighetsvurdering av mottat søknad fra fordeler starter")

            try {
                val personGalleri = gyldigSoeknad.hentPersongalleriFraSoeknad(packet["@skjema_info"])
                val gyldighetsVurdering = gyldigSoeknad.vurderGyldighet(personGalleri)
                val erGyldigFramsatt = (gyldighetsVurdering.resultat == VurderingsResultat.OPPFYLT)
                logger.info("Gyldighetsvurdering I lesGyldigsoeknad: {}", gyldighetsVurdering)

                val sak = behandling.skaffSak(packet["@fnr_soeker"].asText(), packet["@skjema_info"]["type"].asText())
                val behandlingsid = behandling.initierBehandling(
                    sak, packet["@skjema_info"]["mottattDato"].asText(), personGalleri
                )
                behandling.lagreGyldighetsVurdering(behandlingsid, gyldighetsVurdering)
                packet.eventName = "GYLDIG_SOEKNAD:VURDERT"
                packet["sakId"] = sak
                packet["behandlingId"] = behandlingsid
                packet["gyldigInnsender"] = erGyldigFramsatt

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