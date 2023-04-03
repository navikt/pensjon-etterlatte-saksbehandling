package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import java.time.LocalDate
import java.util.*

class TrygdetidService(
    private val trygdetidRepository: TrygdetidRepository,
    private val behandlingKlient: BehandlingKlient
) {
    fun hentTrygdetid(behandlingsId: UUID): Trygdetid? = trygdetidRepository.hentTrygdetid(behandlingsId)

    suspend fun opprettTrygdetid(behandlingId: UUID, bruker: Bruker): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            trygdetidRepository.hentTrygdetid(behandlingId)?.let {
                throw IllegalArgumentException("Trygdetid finnes allerede for behandling $behandlingId")
            }
            trygdetidRepository.opprettTrygdetid(behandlingId)
        }

    suspend fun lagreTrygdetidGrunnlag(
        behandlingId: UUID,
        bruker: Bruker,
        trygdetidGrunnlag: TrygdetidGrunnlag
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            // TODO hvis status er "forbi" trygdetid bør dette sette tilstand tilbake til trygdetid?
            val eksisterendeTrygdetid = trygdetidRepository.hentEnkeltTrygdetidGrunnlag(trygdetidGrunnlag.id)
            val lagretTrygdetidGrunnlag = if (eksisterendeTrygdetid != null) {
                val (antallDager, regelResultat) = beregnAntallDagerTrygdetidGrunnlag(
                    trygdetidGrunnlag.periode,
                    trygdetidGrunnlag.kilde
                )
                val beregnetTrygdetidGrunnlag = trygdetidGrunnlag.copy(trygdetid = antallDager)
                trygdetidRepository.oppdaterTrygdetidGrunnlag(behandlingId, beregnetTrygdetidGrunnlag)
            } else {
                val (antallDager, regelResultat) = beregnAntallDagerTrygdetidGrunnlag(
                    trygdetidGrunnlag.periode,
                    trygdetidGrunnlag.kilde
                )
                val beregnetTrygdetidGrunnlag = trygdetidGrunnlag.copy(trygdetid = antallDager)
                trygdetidRepository.opprettTrygdetidGrunnlag(behandlingId, beregnetTrygdetidGrunnlag)
            }

            val beregnetTrygdetid = beregnTrygdetid(lagretTrygdetidGrunnlag.trygdetidGrunnlag)
            trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid)
        }

    suspend fun lagreBeregnetTrygdetid(
        behandlingId: UUID,
        bruker: Bruker,
        beregnetTrygdetid: BeregnetTrygdetid
    ): Trygdetid =
        tilstandssjekk(behandlingId, bruker) {
            // TODO hvis status er "forbi" trygdetid bør dette sette tilstand tilbake til trygdetid?
            trygdetidRepository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid)
        }

    private suspend fun tilstandssjekk(behandlingId: UUID, bruker: Bruker, block: suspend () -> Trygdetid): Trygdetid {
        val kanFastsetteTrygdetid = behandlingKlient.kanBeregnes(behandlingId, bruker)
        return if (kanFastsetteTrygdetid) {
            block()
        } else {
            throw Exception("Kan ikke opprette/endre trygdetid da behandlingen er i feil tilstand")
        }
    }

    private fun beregnTrygdetid(trygdetidGrunnlag: List<TrygdetidGrunnlag>): BeregnetTrygdetid {
        val grunnlagNasjonalTrygdetid = TrygdetidDelberegningerGrunnlag(
            FaktumNode(
                verdi = trygdetidGrunnlag.filter { it.type == TrygdetidType.NASJONAL }.mapNotNull { it.trygdetid },
                kilde = "System",
                beskrivelse = "Delberegninger nasjonal trygdetid"
            )
        )

        val grunnlagFremtidigTrygdetid = TrygdetidDelberegningerGrunnlag(
            FaktumNode(
                verdi = trygdetidGrunnlag.filter { it.type == TrygdetidType.FREMTIDIG }.mapNotNull { it.trygdetid },
                kilde = "System",
                beskrivelse = "Delberegninger fremtidig trygdetid"
            )
        )

        val grunnlagTotalTrygdetid = TrygdetidDelberegningerGrunnlag(
            FaktumNode(
                verdi = trygdetidGrunnlag.mapNotNull { it.trygdetid },
                kilde = "System",
                beskrivelse = "Delberegninger all trygdetid"
            )
        )

        val (nasjonalTrygdetid, _) = kjoerRegelMedGrunnlag(beregnAntallAarTrygdetidFraDager, grunnlagNasjonalTrygdetid)
        val (fremtidigTrygdetid, _) = kjoerRegelMedGrunnlag(
            beregnAntallAarTrygdetidFraDager,
            grunnlagFremtidigTrygdetid
        )
        val (totalTrygdetid, _) = kjoerRegelMedGrunnlag(beregnAntallAarTrygdetidFraDager, grunnlagTotalTrygdetid)
        return BeregnetTrygdetid(nasjonalTrygdetid, fremtidigTrygdetid, totalTrygdetid)
    }

    private fun beregnAntallDagerTrygdetidGrunnlag(
        periode: TrygdetidPeriode,
        kilde: String
    ): Pair<Int, RegelkjoeringResultat<Int>> {
        val grunnlag = TrygdetidPeriodeGrunnlag(
            periodeFra = FaktumNode(
                verdi = periode.fra,
                kilde = kilde,
                beskrivelse = "Startdato for perioden"
            ),
            periodeTil = FaktumNode(
                verdi = periode.til,
                kilde = kilde,
                beskrivelse = "Sluttdato for perioden"
            )
        )

        return kjoerRegelMedGrunnlag(beregnAntallDagerTrygdetidMellomToDatoer, grunnlag)
    }

    private fun <G, S> kjoerRegelMedGrunnlag(regel: Regel<G, S>, grunnlag: G): Pair<S, RegelkjoeringResultat<S>> {
        // TODO her må vi finne ut hva som er riktig periode
        val resultat =
            regel.eksekver(grunnlag, RegelPeriode(LocalDate.now()))
        return when (resultat) {
            is RegelkjoeringResultat.Suksess -> {
                // TODO hva gjør vi med flere perioder i resultatet?
                val antallDager = resultat.periodiserteResultater.first().resultat.verdi
                Pair(antallDager, resultat)
            }
            is RegelkjoeringResultat.UgyldigPeriode -> throw Exception("En feil oppstod under regelkjøring")
        }
    }
}