package no.nav.etterlatte
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.Beregning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Opplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.time.LocalDate
import java.util.*

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String,
) : Behandling {
    override fun leggTilGyldighetsresultat(behandling: UUID, gyldighetsResultat: GyldighetsResultat) {
        runBlocking {
            behandling_app.post<String>("$url/behandlinger/$behandling/lagregyldighetsproeving") {
                contentType(ContentType.Application.Json)
                body = gyldighetsResultat
            }
        }
    }

    override fun leggTilVilkaarsresultat(behandling: UUID, vilkaarResultat: VilkaarResultat) {
         runBlocking {
            behandling_app.post<String>("$url/behandlinger/$behandling/lagrevilkaarsproeving") {
                contentType(ContentType.Application.Json)
                body = vilkaarResultat
            }
        }
    }
    override fun leggTilBeregningsresultat(behandling: UUID, beregningsResultat: BeregningsResultat) {
        runBlocking {
            behandling_app.post<String>("$url/behandlinger/$behandling/beregning") {
                contentType(ContentType.Application.Json)
                //TODO bli enige om struktur på beregning
                //body = beregningsResultat
                body =
                    Beregning(emptyList(), 123)
            }
        }
    }
}

data class LeggTilVilkaarsResultatRequest(
    val vilkaarResultat: VilkaarResultat
)

