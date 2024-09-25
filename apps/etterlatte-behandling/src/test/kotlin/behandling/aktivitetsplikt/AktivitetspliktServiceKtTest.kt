package no.nav.etterlatte.behandling.aktivitetsplikt

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakDao
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktUnntakType
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit
import java.util.UUID

class AktivitetspliktServiceKtTest {
    private val aktivitetspliktUnntakDao: AktivitetspliktUnntakDao = mockk()
    private val aktivitetspliktAktivitetsgradDao: AktivitetspliktAktivitetsgradDao = mockk()

    @AfterEach
    fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun `hentVurderingForSakHelper henter nyeste vurdering for unntak og aktivitet`() {
        val behandlingGammelId = UUID.randomUUID()
        val behandlingNyId = UUID.randomUUID()

        val vurderingGammel =
            AktivitetspliktVurdering(
                aktivitet =
                    listOf(
                        aktivitetsgrad(
                            behandlingId = behandlingGammelId,
                            aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_100,
                        ),
                    ),
                unntak =
                    listOf(
                        unntak(
                            behandlingId = behandlingGammelId,
                            unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                        ),
                    ),
            )

        val vurderingNy =
            AktivitetspliktVurdering(
                aktivitet =
                    listOf(
                        aktivitetsgrad(
                            behandlingId = behandlingNyId,
                            aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                        ),
                    ),
                unntak =
                    listOf(
                        unntak(
                            behandlingId = behandlingNyId,
                            unntak = AktivitetspliktUnntakType.MIDLERTIDIG_SYKDOM,
                        ),
                    ),
            )

        every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingGammelId) } returns
            vurderingGammel.unntak
        every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingGammelId) } returns
            vurderingGammel.aktivitet

        every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingNyId) } returns vurderingNy.unntak
        every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingNyId) } returns vurderingNy.aktivitet

        every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId1) } returns vurderingNy.unntak
        every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId1) } returns vurderingNy.aktivitet

        val samletVurdering = hentVurderingForSakHelper(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao, sakId1)

        Assertions.assertEquals(samletVurdering, vurderingNy)
    }

    @Test
    fun `hentVurderingForSakHelper ignorerer unntak som finnes hvis de ikke er fra nyeste samlede vurdering`() {
        val behandlingGammelId = UUID.randomUUID()
        val behandlingNyId = UUID.randomUUID()
        val kildeGammel = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now().minus(2, ChronoUnit.DAYS))
        val kildeNy = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())

        val vurderingGammel =
            AktivitetspliktVurdering(
                aktivitet =
                    listOf(
                        aktivitetsgrad(
                            behandlingId = behandlingGammelId,
                            aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_100,
                            endret = kildeGammel,
                            opprettet = kildeGammel,
                        ),
                    ),
                unntak =
                    listOf(
                        unntak(
                            behandlingId = behandlingGammelId,
                            unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                            endret = kildeGammel,
                            opprettet = kildeGammel,
                        ),
                    ),
            )

        val vurderingNy =
            AktivitetspliktVurdering(
                aktivitet =
                    listOf(
                        aktivitetsgrad(
                            behandlingId = behandlingNyId,
                            aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50,
                            endret = kildeNy,
                            opprettet = kildeNy,
                        ),
                    ),
                unntak = emptyList(),
            )

        every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingGammelId) } returns
            vurderingGammel.unntak
        every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingGammelId) } returns
            vurderingGammel.aktivitet

        every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingNyId) } returns vurderingNy.unntak
        every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingNyId) } returns vurderingNy.aktivitet

        every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId1) } returns vurderingGammel.unntak
        every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId1) } returns vurderingNy.aktivitet

        val samletVurdering = hentVurderingForSakHelper(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao, sakId1)

        Assertions.assertEquals(samletVurdering, vurderingNy)
    }

    @Test
    fun `hentVurderingForSakHelper ignorerer aktivitet som finnes hvis de ikke er fra nyeste samlede vurdering`() {
        val behandlingGammelId = UUID.randomUUID()
        val behandlingNyId = UUID.randomUUID()
        val kildeGammel = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now().minus(2, ChronoUnit.DAYS))
        val kildeNy = Grunnlagsopplysning.Saksbehandler("Z123456", Tidspunkt.now())

        val vurderingGammel =
            AktivitetspliktVurdering(
                aktivitet =
                    listOf(
                        aktivitetsgrad(
                            behandlingId = behandlingGammelId,
                            aktivitetsgrad = AktivitetspliktAktivitetsgradType.AKTIVITET_100,
                            endret = kildeGammel,
                            opprettet = kildeGammel,
                        ),
                    ),
                unntak =
                    listOf(
                        unntak(
                            behandlingId = behandlingGammelId,
                            unntak = AktivitetspliktUnntakType.OMSORG_BARN_SYKDOM,
                            endret = kildeGammel,
                            opprettet = kildeGammel,
                        ),
                    ),
            )

        val vurderingNy =
            AktivitetspliktVurdering(
                aktivitet = listOf(),
                unntak =
                    listOf(
                        unntak(
                            behandlingId = behandlingNyId,
                            unntak = AktivitetspliktUnntakType.MIDLERTIDIG_SYKDOM,
                            endret = kildeNy,
                            opprettet = kildeNy,
                        ),
                    ),
            )

        every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingGammelId) } returns
            vurderingGammel.unntak
        every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingGammelId) } returns
            vurderingGammel.aktivitet

        every { aktivitetspliktUnntakDao.hentUnntakForBehandling(behandlingNyId) } returns vurderingNy.unntak
        every { aktivitetspliktAktivitetsgradDao.hentAktivitetsgradForBehandling(behandlingNyId) } returns vurderingNy.aktivitet

        every { aktivitetspliktUnntakDao.hentNyesteUnntak(sakId1) } returns vurderingNy.unntak
        every { aktivitetspliktAktivitetsgradDao.hentNyesteAktivitetsgrad(sakId1) } returns vurderingGammel.aktivitet

        val samletVurdering = hentVurderingForSakHelper(aktivitetspliktAktivitetsgradDao, aktivitetspliktUnntakDao, sakId1)

        Assertions.assertEquals(samletVurdering, vurderingNy)
    }
}
