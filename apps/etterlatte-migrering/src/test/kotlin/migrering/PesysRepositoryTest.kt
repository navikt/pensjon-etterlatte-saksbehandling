package no.nav.etterlatte.migrering

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.rapidsandrivers.migrering.AvdoedForelder
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PesysRepositoryTest(datasource: DataSource) {
    private val repository: PesysRepository = PesysRepository(datasource)

    @Test
    fun `lagre kobling til behandlingid`() {
        val behandlingId = UUID.randomUUID()
        sakMedKobling(pesysSak(123L), behandlingId, 321L)

        val kobling = repository.hentKoplingTilBehandling(PesysId(123L))

        assertEquals(behandlingId, kobling!!.behandlingId)
        assertEquals(321L, kobling.sakId)
    }

    @Test
    fun `Skal oppdatere migreringsstatus til ferdig naar brevdistribusjon er ferdig foerst`() {
        val behandlingId = UUID.randomUUID()
        sakMedKobling(pesysSak(123L), behandlingId, 321L)

        repository.oppdaterStatus(PesysId(123L), Migreringsstatus.BREVUTSENDING_OK)
        assertEquals(Migreringsstatus.BREVUTSENDING_OK, repository.hentStatus(123L))

        repository.oppdaterStatus(PesysId(123L), Migreringsstatus.UTBETALING_OK)
        assertEquals(Migreringsstatus.FERDIG, repository.hentStatus(123L))
    }

    @Test
    fun `Skal oppdatere migreringsstatus til ferdig naar utbetaling er godkjent foerst`() {
        val behandlingId = UUID.randomUUID()
        sakMedKobling(pesysSak(123L), behandlingId, 321L)

        repository.oppdaterStatus(PesysId(123L), Migreringsstatus.UTBETALING_OK)
        assertEquals(Migreringsstatus.UTBETALING_OK, repository.hentStatus(123L))

        repository.oppdaterStatus(PesysId(123L), Migreringsstatus.BREVUTSENDING_OK)
        assertEquals(Migreringsstatus.FERDIG, repository.hentStatus(123L))
    }

    @Test
    fun `lagre kobling til behandlingid oppdateres til ny behandlingsid`() {
        sakMedKobling(pesysSak(123L), UUID.randomUUID(), 321L)
        val nyBehandlingId = UUID.randomUUID()
        sakMedKobling(pesysSak(123L), nyBehandlingId, 321L)

        val nyKobling = repository.hentKoplingTilBehandling(PesysId(123L))

        assertEquals(nyBehandlingId, nyKobling!!.behandlingId)
    }

    @Test
    fun `skal oppdatere pesyssak med gjenopprettes_automatisk`() {
        val psak = pesysSak(123L)
        repository.lagrePesyssak(psak)

        val request =
            psak.tilMigreringsrequest().copy(
                kanAutomatiskGjenopprettes = true,
            )
        repository.oppdaterKanGjenopprettesAutomatisk(request)
    }

    @Test
    fun `lagre manuell migrering`() {
        val pesysId = 123L

        repository.lagreManuellMigrering(pesysId)
        val manuellMigrering = repository.hentStatus(pesysId)

        assertEquals(Migreringsstatus.UNDER_MIGRERING_MANUELT, manuellMigrering)
    }

    @Test
    fun `Lagre gyldige dry runs flere ganger`() {
        val pesyssak = pesysSak(123L)
        repository.lagrePesyssak(pesyssak)
        repository.lagreGyldigDryRun(pesyssak.tilMigreringsrequest())
        repository.lagreGyldigDryRun(pesyssak.tilMigreringsrequest())
    }

    private fun sakMedKobling(
        pesyssak: Pesyssak,
        behandlingsId: UUID,
        sakId: Long,
    ) {
        repository.lagrePesyssak(pesyssak)
        repository.oppdaterStatus(PesysId(pesyssak.id), Migreringsstatus.UNDER_MIGRERING)
        repository.lagreKoplingTilBehandling(behandlingsId, PesysId(pesyssak.id), sakId)
    }

    companion object {
        private fun pesysSak(id: Long) =
            Pesyssak(
                id = id,
                enhet = Enhet("enhet"),
                soeker = Folkeregisteridentifikator.of("09498230323"),
                gjenlevendeForelder = Folkeregisteridentifikator.of("09498230323"),
                avdoedForelder = listOf(AvdoedForelder(Folkeregisteridentifikator.of("09498230323"), Tidspunkt.now())),
                foersteVirkningstidspunkt = YearMonth.of(2024, 1),
                beregning = Beregning(0, 0, 0, Tidspunkt.now(), 0, IntBroek(0, 0)),
                trygdetid = Trygdetid(emptyList()),
                dodAvYrkesskade = false,
                flyktningStatus = false,
                spraak = Spraak.NB,
            )
    }
}
