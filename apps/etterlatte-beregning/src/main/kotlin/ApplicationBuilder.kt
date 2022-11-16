import no.nav.etterlatte.model.BeregningService
import no.nav.etterlatte.restModule
import no.nav.helse.rapids_rivers.RapidApplication
import java.util.*

class ApplicationBuilder {
    private val env = System.getenv()
    private val beregningService = BeregningService()
    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env.withConsumerGroupId()))
            .withKtorModule {
                restModule(
                    beregningService = beregningService
                )
            }
            .build()
    // .apply { registrerVedlikeholdsriver() } TODO?

    fun start() = rapidsConnection.start()

    private fun publiser(melding: String, key: UUID) {
        rapidsConnection.publish(message = melding, key = key.toString())
    }
}

fun Map<String, String>.withConsumerGroupId() =
    this.toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }