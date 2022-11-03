package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerRequest
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.saksbehandling.api.typer.klientside.DetaljertBehandlingDto
import no.nav.etterlatte.saksbehandling.api.typer.klientside.Familieforhold
import no.nav.etterlatte.typer.LagretHendelser
import no.nav.etterlatte.typer.Saker
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

data class PersonSakerResult(
    val person: Person,
    val behandlingListe: BehandlingListe,
    val grunnlagsendringshendelser: GrunnlagsendringshendelseListe = GrunnlagsendringshendelseListe(emptyList())
)

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
    private val pdlKlient: PdltjenesterKlient,
    private val vedtakKlient: EtterlatteVedtak,
    private val grunnlagKlient: EtterlatteGrunnlag
) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentPersonOgSaker(fnr: String, accessToken: String): PersonSakerResult {
        logger.info("Henter person med tilhørende behandlinger")

        try {
            val person = pdlKlient.hentPerson(fnr, accessToken)
            val saker = behandlingKlient.hentSakerForPerson(fnr, accessToken)
            val sakIder = saker.saker.map { it.id }.distinct()

            val behandlinger = sakIder.map { behandlingKlient.hentBehandlingerForSak(it.toInt(), accessToken) }
                .flatMap { it.behandlinger }

            val behandlingsListe = BehandlingListe(behandlinger)
            val grunnlagsendringshendelser = hentGrunnlagsendrinshendelser(sakIder, accessToken)

            return PersonSakerResult(person, behandlingsListe, grunnlagsendringshendelser)
        } catch (e: InvalidFoedselsnummer) {
            logger.error("Henting av person fra pdl feilet pga ugyldig fødselsnummer", e)
            throw e
        } catch (e: Exception) {
            logger.error("Henting av person med tilhørende behandlinger feilet", e)
            throw e
        }
    }

    private suspend fun hentGrunnlagsendrinshendelser(
        sakIder: List<Long>,
        accessToken: String
    ): GrunnlagsendringshendelseListe {
        return try {
            val grunnlagsendringshendelser = sakIder.flatMap {
                behandlingKlient.hentGrunnlagsendringshendelserForSak(it.toInt(), accessToken).hendelser
            }
            GrunnlagsendringshendelseListe(hendelser = grunnlagsendringshendelser)
        } catch (e: Exception) {
            logger.error(
                "Fikk ikke hentet grunnlagsendringshendelser for saker med " +
                    "ider ${sakIder.joinToString { it.toString() }}",
                e
            )
            GrunnlagsendringshendelseListe(emptyList())
        }
    }

    suspend fun hentSaker(accessToken: String): Saker {
        logger.info("Henter alle saker")
        return behandlingKlient.hentSaker(accessToken)
    }

    suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingListe {
        logger.info("Henter behandlinger for sak $sakId")
        return behandlingKlient.hentBehandlingerForSak(sakId, accessToken)
    }

    suspend fun hentBehandling(behandlingId: String, accessToken: String) = coroutineScope {
        logger.info("Henter behandling")
        val behandling = async { behandlingKlient.hentBehandling(behandlingId, accessToken) }
        val vedtak = async { vedtakKlient.hentVedtak(behandlingId, accessToken) }
        val hendelser = async { behandlingKlient.hentHendelserForBehandling(behandlingId, accessToken) }
        val sakId = behandling.await().sak
        val avdoed = async { grunnlagKlient.finnPersonOpplysning(sakId, AVDOED_PDL_V1, accessToken) }
        val gjenlevende = async { grunnlagKlient.finnPersonOpplysning(sakId, GJENLEVENDE_FORELDER_PDL_V1, accessToken) }
        val soeker = async { grunnlagKlient.finnPersonOpplysning(sakId, SOEKER_PDL_V1, accessToken) }

        DetaljertBehandlingDto(
            id = behandling.await().id,
            sak = sakId,
            gyldighetsprøving = behandling.await().gyldighetsproeving,
            kommerSoekerTilgode = vedtak.await().kommerSoekerTilgodeResultat,
            vilkårsprøving = vedtak.await().vilkaarsResultat,
            beregning = vedtak.await().beregningsResultat,
            avkortning = vedtak.await().avkortingsResultat,
            saksbehandlerId = vedtak.await().saksbehandlerId,
            fastsatt = vedtak.await().vedtakFattet,
            datoFattet = vedtak.await().datoFattet,
            datoattestert = vedtak.await().datoattestert,
            attestant = vedtak.await().attestant,
            soeknadMottattDato = behandling.await().soeknadMottattDato,
            virkningstidspunkt = behandling.await().virkningstidspunkt,
            status = behandling.await().status,
            hendelser = hendelser.await().hendelser,
            familieforhold = Familieforhold(avdoed.await(), gjenlevende.await()),
            behandlingType = behandling.await().behandlingType,
            søker = soeker.await()?.opplysning
        )
    }

    suspend fun slettBehandlinger(sakId: Int, accessToken: String): Boolean {
        return behandlingKlient.slettBehandlinger(sakId, accessToken)
    }

    suspend fun hentHendelserForBehandling(behandlingId: String, accessToken: String): LagretHendelser {
        logger.info("Henter hendelser for behandling $behandlingId")
        return behandlingKlient.hentHendelserForBehandling(behandlingId, accessToken)
    }

    suspend fun slettRevurderinger(sakId: Int, accessToken: String): Boolean {
        logger.error("Ikke bruk dette i prod, slett endepunktet så raskt som mulig! Slett revurderinger er ban")
        return behandlingKlient.slettRevurderinger(sakId, accessToken)
    }

    suspend fun avbrytBehanding(behandlingId: String, accessToken: String): Boolean {
        logger.error("Avbryter behandling med behandingsId $behandlingId")
        return behandlingKlient.avbrytBehandling(behandlingId, accessToken)
    }

    suspend fun opprettManueltOpphoer(
        manueltOpphoerRequest: ManueltOpphoerRequest,
        accessToken: String
    ): ManueltOpphoerResponse {
        logger.info("Oppretter manuelt opphør for sak ${manueltOpphoerRequest.sak}")
        return behandlingKlient.opprettManueltOpphoer(manueltOpphoerRequest, accessToken)
            .getOrThrow()
    }

    suspend fun fastsettVirkningstidspunkt(
        behandlingId: String,
        dato: LocalDateTime,
        accessToken: String
    ): VirkningstidspunktResponse {
        logger.info("Fastsetter virkningstidspunkt for behandling $behandlingId")
        return behandlingKlient.fastsettVirkningstidspunkt(behandlingId, dato.toLocalDate(), accessToken)
    }
}