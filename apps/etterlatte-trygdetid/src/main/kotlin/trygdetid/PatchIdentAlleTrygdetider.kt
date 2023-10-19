package no.nav.etterlatte.trygdetid

import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.jobs.LeaderElection
import no.nav.etterlatte.token.Systembruker
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

class PatchIdentAlleTrygdetider(
    private val grunnlagKlient: GrunnlagKlient,
    private val dataSource: DataSource,
    private val leaderElection: LeaderElection,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun leggPaaIdentDerDetMangler() {
        if (!leaderElection.isLeader()) {
            logger.info("patcher ingen trygdetider siden pod ikke er leader")
            return
        }
        val trygdetiderSomManglerIdent = hentTrygdetiderSomManglerIdent()
        logger.info("Starter patching av ${trygdetiderSomManglerIdent.size} trygdetider som mangler ident")
        trygdetiderSomManglerIdent.forEach {
            try {
                val ident = finnIdentForTrygdetid(it)
                oppdaterIdentForTrygdetid(it, ident)
            } catch (e: Exception) {
                logger.error(
                    "Kunne ikke oppdatere ident for trygdetid med id=${it.trygdetidId} i behandlingen med " +
                        "id=${it.behandlingId} på grunn av feil",
                    e,
                )
            }
        }
    }

    private fun oppdaterIdentForTrygdetid(
        trygdetidIdOgBehandlingId: TrygdetidIdOgBehandlingId,
        ident: String,
    ) {
        using(sessionOf(dataSource)) { session ->
            queryOf(
                "UPDATE trygdetid SET ident = :ident " +
                    "WHERE id = :trygdetidId and behandling_id = :behandlingId and ident is null",
                mapOf(
                    "ident" to ident,
                    "trygdetidId" to trygdetidIdOgBehandlingId.trygdetidId,
                    "behandlingId" to trygdetidIdOgBehandlingId.behandlingId,
                ),
            )
                .let {
                    session.run(it.asUpdate)
                }
        }
    }

    private fun finnIdentForTrygdetid(trygdetidIdOgBehandlingId: TrygdetidIdOgBehandlingId): String {
        val grunnlag =
            runBlocking {
                grunnlagKlient.hentGrunnlag(
                    sakId = trygdetidIdOgBehandlingId.sakId,
                    behandlingId = trygdetidIdOgBehandlingId.behandlingId,
                    brukerTokenInfo = Systembruker("trygdetid", "trygdetid"),
                )
            }
        return requireNotNull(grunnlag.hentAvdoed().hentFoedselsnummer()?.verdi?.value) {
            "Kunne ikke hente avdøds fødselsnummer for behandling med id=${trygdetidIdOgBehandlingId.behandlingId}"
        }
    }

    private fun hentTrygdetiderSomManglerIdent(): List<TrygdetidIdOgBehandlingId> {
        return using(sessionOf(dataSource)) { session ->
            queryOf("SELECT id, behandling_id, sak_id FROM trygdetid WHERE ident is null", null)
                .let {
                    session.run(
                        it.map { row ->
                            TrygdetidIdOgBehandlingId(
                                trygdetidId = row.uuid("id"),
                                behandlingId = row.uuid("behandling_id"),
                                sakId = row.long("sak_id"),
                            )
                        }.asList,
                    )
                }
        }
    }
}

data class TrygdetidIdOgBehandlingId(
    val trygdetidId: UUID,
    val behandlingId: UUID,
    val sakId: Long,
)
