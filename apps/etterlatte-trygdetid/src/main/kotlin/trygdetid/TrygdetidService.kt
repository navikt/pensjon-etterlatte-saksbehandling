package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning.RegelKilde
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import java.util.*

class TrygdetidService(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregnTrygdetidService: TrygdetidBeregningService
) {
    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? = trygdetidRepository.hentTrygdetid(behandlingsId)

    suspend fun opprettTrygdetid(behandlingId: UUID, bruker: Bruker): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            trygdetidRepository.hentTrygdetid(behandlingId)?.let {
                throw IllegalArgumentException("Trygdetid finnes allerede for behandling $behandlingId")
            }

            val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
            val avdoed = grunnlagKlient.hentGrunnlag(behandling.sak, bruker).hentAvdoed()
            val trygdetid = Trygdetid(
                sakId = behandling.sak,
                behandlingId = behandling.id,
                opplysninger = hentOpplysninger(avdoed)
            )

            trygdetidRepository.opprettTrygdetid(trygdetid)
        }

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        bruker: Bruker,
        trygdetidGrunnlag: TrygdetidGrunnlag
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            val trygdetidGrunnlagBeregnet = trygdetidGrunnlag.oppdaterBeregnetTrygdetid(
                beregnTrygdetidService.beregnTrygdetidGrunnlag(trygdetidGrunnlag)
            )

            val gjeldendeTrygdetid = trygdetidRepository.hentTrygdetid(behandlingId)
                ?: throw Exception("Fant ikke gjeldende trygdetid for behandlingId=$behandlingId")

            val trygdetidMedOppdatertTrygdetidGrunnlag =
                gjeldendeTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlagBeregnet)

            val beregnetTrygdetid = beregnTrygdetidService.beregnTrygdetid(
                trygdetidMedOppdatertTrygdetidGrunnlag.trygdetidGrunnlag
            )

            val oppdatertTrygdetid =
                trygdetidMedOppdatertTrygdetidGrunnlag.oppdaterBeregnetTrygdetid(beregnetTrygdetid)

            trygdetidRepository.oppdaterTrygdetid(oppdatertTrygdetid).also {
                behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, bruker)
            }
        }

    suspend fun kopierSisteTrygdetidberegning(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        bruker: Bruker
    ): Trygdetid {
        val forrigeTrygdetid = requireNotNull(hentTrygdetid(forrigeBehandlingId)) {
            "Fant ingen trygdetid for behandlingId=$forrigeBehandlingId"
        }
        val regulering = behandlingKlient.hentBehandling(behandlingId, bruker)

        val kopiertTrygdetid = Trygdetid(
            sakId = regulering.sak,
            behandlingId = regulering.id,
            opplysninger = forrigeTrygdetid.opplysninger,
            trygdetidGrunnlag = forrigeTrygdetid.trygdetidGrunnlag,
            beregnetTrygdetid = forrigeTrygdetid.beregnetTrygdetid
        )

        return trygdetidRepository.opprettTrygdetid(kopiertTrygdetid)
    }

    private fun kildeFoedselsnummer(): RegelKilde = RegelKilde(
        "Beregnet basert på fødselsdato fra pdl",
        Tidspunkt.now(),
        "1"
    )

    private fun hentOpplysninger(avdoed: Grunnlagsdata<JsonNode>): List<Opplysningsgrunnlag> {
        val foedselsdato = avdoed.hentFoedselsdato()
        val opplysninger = listOf(
            Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FOEDSELSDATO, foedselsdato?.kilde, foedselsdato?.verdi),
            Opplysningsgrunnlag.ny(
                TrygdetidOpplysningType.FYLT_16,
                kildeFoedselsnummer(),
                // Ifølge paragraf § 3-5 regnes trygdetid fra tidspunkt en person er fylt 16 år
                foedselsdato?.verdi?.plusYears(16)
            ),
            Opplysningsgrunnlag.ny(
                TrygdetidOpplysningType.FYLLER_66,
                kildeFoedselsnummer(),
                // Ifølge paragraf § 3-5 regnes trygdetid frem til tidspunkt en person er fyller 66 pår
                foedselsdato?.verdi?.plusYears(66)
            ),
            avdoed.hentDoedsdato().let {
                Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, it?.kilde, it?.verdi)
            }
        )
        return opplysninger
    }

    private suspend fun tilstandssjekk(behandlingId: UUID, bruker: Bruker, block: suspend () -> Trygdetid): Trygdetid {
        val kanFastsetteTrygdetid = behandlingKlient.kanBeregnes(behandlingId, bruker)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            throw Exception("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")
        }
    }
}