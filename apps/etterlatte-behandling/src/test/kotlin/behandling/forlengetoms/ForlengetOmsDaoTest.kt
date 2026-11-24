package no.nav.etterlatte.behandling.forlengetoms

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.SQLException
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
class ForlengetOmsDaoTest(
    ds: DataSource,
) {
    private val connection = ConnectionAutoclosingTest(ds)
    private val dao = ForlengetOmsDao(connection)
    private val sakDao = SakSkrivDao(SakendringerDao(connection))
    private val behandlingDao =
        BehandlingDao(
            KommerBarnetTilGodeDao(connection),
            RevurderingDao(connection),
            connection,
        )
    private val kilde = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())
    private val utdanning = ForlengetOmsAktivitet.Utdanning("Skole", "Studium", JaNei.JA)

    @Test
    fun `lagrer og henter relevante svar for alle aktivitetstyper`() {
        val aktiviteter =
            listOf(
                utdanning to ForlengetOmsUtfall.INNVILGELSE,
                ForlengetOmsAktivitet.ArbeidsrettetTiltak("Tiltak", Tiltaksomfang.DELTID, JaNei.NEI) to
                    ForlengetOmsUtfall.INNVILGELSE,
                ForlengetOmsAktivitet.IngenAktivitet to ForlengetOmsUtfall.AVSLAG,
            )

        aktiviteter.forEach { (aktivitet, utfall) ->
            val behandlingId = nyBehandling()
            dao.hentVurdering(behandlingId) shouldBe null

            dao.lagreVurdering(behandlingId, aktivitet, "Begrunnelse", utfall, kilde)

            dao.hentVurdering(behandlingId) shouldBe
                ForlengetOmsVurdering(behandlingId, aktivitet, "Begrunnelse", utfall, kilde, kilde)
        }
    }

    @Test
    fun `oppdaterer siste vurdering og endret men beholder opprettet`() {
        val behandlingId = nyBehandling()
        dao.lagreVurdering(behandlingId, utdanning, "Begrunnelse", ForlengetOmsUtfall.INNVILGELSE, kilde)

        val lagretAv = Grunnlagsopplysning.Saksbehandler("Z654321", kilde.tidspunkt.plus(1, ChronoUnit.HOURS))
        dao.lagreVurdering(
            behandlingId,
            ForlengetOmsAktivitet.IngenAktivitet,
            "Ny begrunnelse",
            ForlengetOmsUtfall.AVSLAG,
            lagretAv,
        )

        dao.hentVurdering(behandlingId) shouldBe
            ForlengetOmsVurdering(
                behandlingId,
                ForlengetOmsAktivitet.IngenAktivitet,
                "Ny begrunnelse",
                ForlengetOmsUtfall.AVSLAG,
                kilde,
                lagretAv,
            )
    }

    @Test
    fun `kan ikke lagre vurdering uten tilhoerende behandling`() {
        val behandlingId = UUID.randomUUID()

        assertThrows<SQLException> {
            dao.lagreVurdering(behandlingId, utdanning, "Begrunnelse", ForlengetOmsUtfall.INNVILGELSE, kilde)
        }
        dao.hentVurdering(behandlingId) shouldBe null
    }

    private fun nyBehandling(): UUID {
        val sak = sakDao.opprettSak("12345678901", SakType.OMSTILLINGSSTOENAD, Enheter.defaultEnhet.enhetNr)
        val behandling =
            opprettBehandling(
                type = BehandlingType.REVURDERING,
                sakId = sak.id,
                revurderingAarsak = Revurderingaarsak.FORLENGET_OMS,
            )
        behandlingDao.opprettBehandling(behandling)
        return behandling.id
    }
}
