package behandling

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingKlient
import no.nav.etterlatte.behandling.OppgaveService
import no.nav.etterlatte.behandling.Vedtak
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.OppgaveStatus
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingResultatDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfallDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VirkningstidspunktDto
import no.nav.etterlatte.typer.BehandlingsOppgave
import no.nav.etterlatte.typer.OppgaveListe
import no.nav.etterlatte.typer.Sak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*

internal class OppgaveServiceTest {
    @MockK
    lateinit var behandlingKlient: BehandlingKlient

    @InjectMockKs
    lateinit var service: OppgaveService

    @BeforeEach
    fun setUp() = MockKAnnotations.init(this)
    private val accessToken = UUID.randomUUID().toString()

    @Test
    fun hentAlleOppgaver() {
        val vedtak = Vedtak(
            "4",
            UUID.randomUUID(),
            null,
            null,
            null,
            VilkaarsvurderingDto(
                UUID.randomUUID(),
                emptyList(),
                VirkningstidspunktDto(
                    YearMonth.of(2022, 1),
                    Grunnlagsopplysning.Saksbehandler("Z1000", Instant.now()).toJsonNode()
                ),
                VilkaarsvurderingResultatDto(
                    VilkaarsvurderingUtfallDto.OPPFYLT,
                    "",
                    LocalDateTime.now(),
                    "ABV"
                )
            ),
            null,
            null,
            null,
            null,
            null
        )
        val behandlingId = UUID.randomUUID()
        coEvery { behandlingKlient.hentOppgaver(accessToken) } returns OppgaveListe(
            oppgaver = listOf(
                BehandlingsOppgave(
                    behandlingId,
                    BehandlingStatus.UNDER_BEHANDLING,
                    OppgaveStatus.NY,
                    Sak("ident", "saktype", 1),
                    ZonedDateTime.now(),
                    LocalDate.now(),
                    BehandlingType.REVURDERING,
                    1
                )
            )
        )

        val resultat = runBlocking { service.hentAlleOppgaver(accessToken) }

        assertEquals(BehandlingStatus.UNDER_BEHANDLING, resultat.oppgaver.first().status)
        assertEquals(BehandlingType.REVURDERING, resultat.oppgaver.first().behandlingType)
        assertEquals(OppgaveStatus.NY, resultat.oppgaver.first().oppgaveStatus)
    }
}