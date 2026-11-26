package no.nav.etterlatte.common

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.BehandlingsHendelserKafkaProducerImpl
import no.nav.etterlatte.behandling.domain.toStatistikkBehandling
import no.nav.etterlatte.foerstegangsbehandling
import no.nav.etterlatte.kafka.TestProdusent
import no.nav.etterlatte.libs.common.behandling.BehandlingHendelseType
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.withSerializableRetry
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class DatabaseContextIntegrationTest(
    private val dataSource: DataSource,
) {
    @AfterEach
    fun ryddKontekst() {
        Kontekst.remove()
    }

    @Test
    fun `ruller tilbake hele transaksjonen og sender bare hendelser fra vellykket forsoek`() =
        runBlocking<Unit> {
            val sakId =
                dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement
                            .executeQuery(
                                "INSERT INTO sak (fnr, saktype, enhet) VALUES ('retry-test', 'BARNEPENSJON', '4806') RETURNING id",
                            ).use {
                                it.next()
                                SakId(it.getLong(1))
                            }
                    }
                }
            val kontekst = nyKontekstMedBrukerOgDatabase(Self("retry-test"), dataSource)
            val produsent = TestProdusent<String, String>()
            val hendelser = BehandlingsHendelserKafkaProducerImpl(produsent)
            val statistikkBehandling =
                foerstegangsbehandling(sakId = sakId).toStatistikkBehandling(Persongalleri("retry-test"))
            var forsoek = 0

            withSerializableRetry {
                kontekst.databasecontxt.inTransaction {
                    forsoek++
                    val connection = kontekst.databasecontxt.activeTx()
                    connection.prepareStatement("SELECT enhet FROM sak WHERE id = ?").use {
                        it.setLong(1, sakId.sakId)
                        it.executeQuery().close()
                    }
                    connection.prepareStatement("INSERT INTO oppgave (id, sak_id, referanse) VALUES (?, ?, ?)").use {
                        it.setObject(1, UUID.randomUUID())
                        it.setLong(2, sakId.sakId)
                        it.setString(3, "forsoek-$forsoek")
                        it.executeUpdate()
                    }
                    hendelser.sendMeldingForHendelseStatistikk(statistikkBehandling, BehandlingHendelseType.OPPRETTET)
                    hendelser.sendMeldingForEndretEnhet("forsoek-$forsoek", Enheter.defaultEnhet.enhetNr)
                    assertTrue(produsent.publiserteMeldinger.isEmpty())

                    if (forsoek == 1) {
                        dataSource.connection.use { konkurrerendeConnection ->
                            konkurrerendeConnection.prepareStatement("UPDATE sak SET enhet = '4815' WHERE id = ?").use {
                                it.setLong(1, sakId.sakId)
                                it.executeUpdate()
                            }
                        }
                    }
                    connection.prepareStatement("UPDATE sak SET enhet = '4806' WHERE id = ?").use {
                        it.setLong(1, sakId.sakId)
                        it.executeUpdate()
                    }
                }
            }

            assertEquals(2, forsoek)
            assertEquals(2, produsent.publiserteMeldinger.size)
            assertEquals("forsoek-2", produsent.publiserteMeldinger.last().noekkel)
            dataSource.connection.use { connection ->
                connection.prepareStatement("SELECT referanse FROM oppgave WHERE sak_id = ?").use {
                    it.setLong(1, sakId.sakId)
                    it.executeQuery().use { oppgaver ->
                        assertTrue(oppgaver.next())
                        assertEquals("forsoek-2", oppgaver.getString(1))
                        assertTrue(!oppgaver.next())
                    }
                }
            }
        }
}
