package no.nav.etterlatte.sak

import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService.SakMedEnhet
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.jobs.LeaderElection
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.concurrent.thread

class SettSakEnhetJobb(
    val sakService: SakService,
    private val dataSource: DataSource,
    private val leaderElection: LeaderElection
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun schedule() {
        thread {
            TimeUnit.SECONDS.sleep(120)
            run()
        }
    }

    private fun run() {
        val correlationId = UUID.randomUUID().toString()

        if (leaderElection.isLeader()) {
            withLogContext(correlationId) {
                log.info("Starter jobb ${this::class.simpleName}")

                val connection = dataSource.connection

                val hentSakerStatement =
                    connection.prepareStatement("SELECT id, sakType, fnr, enhet FROM sak WHERE enhet IS NULL")

                val saker = hentSakerStatement.executeQuery().toList {
                    Sak(
                        sakType = enumValueOf(getString("sakType")),
                        ident = getString("fnr"),
                        id = getLong("id"),
                        enhet = getString("enhet").takeUnless { wasNull() }
                    )
                }

                log.info("${saker.size} saker uten enhet")

                val sakerMedEnhet = saker.map { sak ->
                    SakMedEnhet(
                        sak.id,
                        sakService.finnEnhetForPersonOgTema(sak.ident, sak.sakType.tema, sak.sakType).enhetNr
                    )
                }

                val oppdaterSakerStatement = connection.prepareStatement(
                    """
                        UPDATE sak 
                        set enhet = ? 
                        where id = ?
                    """.trimIndent()
                )

                sakerMedEnhet.forEach {
                    oppdaterSakerStatement.setString(1, it.enhet)
                    oppdaterSakerStatement.setLong(2, it.id)
                    oppdaterSakerStatement.addBatch()
                }

                oppdaterSakerStatement.executeBatch()
            }
        }
    }
}