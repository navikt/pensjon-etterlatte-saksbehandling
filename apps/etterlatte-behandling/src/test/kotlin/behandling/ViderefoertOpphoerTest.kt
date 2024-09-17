package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.ConnectionAutoclosingTest
import no.nav.etterlatte.DatabaseContextTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.behandling.kommerbarnettilgode.KommerBarnetTilGodeDao
import no.nav.etterlatte.behandling.revurdering.RevurderingDao
import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.nyKontekstMedBrukerOgDatabaseContext
import no.nav.etterlatte.opprettBehandling
import no.nav.etterlatte.sak.SakSkrivDao
import no.nav.etterlatte.sak.SakendringerDao
import no.nav.etterlatte.tilgangsstyring.SaksbehandlerMedRoller
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
class ViderefoertOpphoerTest(
    private val dataSource: DataSource,
) {
    val user = mockk<SaksbehandlerMedEnheterOgRoller>()
    val saksbehandlerMedRoller =
        mockk<SaksbehandlerMedRoller> {
            every { harRolleStrengtFortrolig() } returns false
            every { harRolleEgenAnsatt() } returns true
        }
    lateinit var connection: ConnectionAutoclosingTest
    lateinit var service: BehandlingService
    lateinit var behandlingDao: BehandlingDao
    private val saksbehandlerKilde: Grunnlagsopplysning.Saksbehandler =
        Grunnlagsopplysning.Saksbehandler.create("A123")

    @BeforeEach
    fun setUp() {
        every { user.saksbehandlerMedRoller } returns saksbehandlerMedRoller
        every { user.name() } returns "User"
        nyKontekstMedBrukerOgDatabaseContext(user, DatabaseContextTest(dataSource))

        val kommerBarnetTilGodeDao =
            mockk<KommerBarnetTilGodeDao>().also { every { it.hentKommerBarnetTilGode(any()) } returns null }
        connection = ConnectionAutoclosingTest(dataSource)
        behandlingDao = BehandlingDao(kommerBarnetTilGodeDao, mockk<RevurderingDao>(), connection)
        service =
            BehandlingServiceImpl(
                behandlingDao = behandlingDao,
                behandlingHendelser = mockk(),
                grunnlagsendringshendelseDao = mockk(),
                hendelseDao = mockk(),
                grunnlagKlient = mockk(),
                behandlingRequestLogger = mockk(),
                kommerBarnetTilGodeDao = kommerBarnetTilGodeDao,
                oppgaveService = mockk(),
                grunnlagService = mockk(),
                beregningKlient = mockk(),
            )
    }

    @Test
    fun `lagrer viderefoert opphoer`() {
        val sak =
            SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() }).opprettSak(
                SOEKER_FOEDSELSNUMMER.value,
                SakType.BARNEPENSJON,
                Enhet.defaultEnhet,
            )
        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak.id)
        behandlingDao.opprettBehandling(behandling = opprettBehandling)
        val opphoerstidspunkt = YearMonth.of(2024, Month.JUNE)
        service.oppdaterViderefoertOpphoer(
            behandlingId = opprettBehandling.id,
            viderefoertOpphoer =
                viderefoertOpphoer(
                    skalViderefoere = JaNei.JA,
                    behandlingId = opprettBehandling.id,
                    opphoersdato = opphoerstidspunkt,
                ),
        )
        val viderefoertOpphoer = behandlingDao.hentViderefoertOpphoer(opprettBehandling.id)!!
        assertEquals(opprettBehandling.id, viderefoertOpphoer.behandlingId)
        assertEquals(opphoerstidspunkt, viderefoertOpphoer.dato)
    }

    @Test
    fun `inaktiverer opphør`() {
        val sak =
            SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() }).opprettSak(
                SOEKER_FOEDSELSNUMMER.value,
                SakType.BARNEPENSJON,
                Enhet.defaultEnhet,
            )
        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak.id)
        behandlingDao.opprettBehandling(behandling = opprettBehandling)

        val viderefoertOpphoer =
            viderefoertOpphoer(
                behandlingId = opprettBehandling.id,
                opphoersdato = YearMonth.of(2024, Month.JANUARY),
                skalViderefoere = JaNei.JA,
                vilkaar = VilkaarType.BP_FORTSATT_MEDLEMSKAP_UNNTAK_AVDOED_MINDRE_20_AAR_BOTID_RETT_TILLEGGSPENSJON,
            )
        service.oppdaterViderefoertOpphoer(
            behandlingId = opprettBehandling.id,
            viderefoertOpphoer =
            viderefoertOpphoer,
        )

        service.fjernViderefoertOpphoer(
            opprettBehandling.id,
            Grunnlagsopplysning.Saksbehandler.create("Slettersen"),
        )

        val alleViderefoertOpphoer = hentAlleViderefoertOpphoer(opprettBehandling.id)

        alleViderefoertOpphoer
            .filter { it.aktiv } shouldBe emptyList()

        val inaktivert = alleViderefoertOpphoer.single { !it.aktiv }
        (inaktivert.kilde as Grunnlagsopplysning.Saksbehandler).ident shouldBe "Slettersen"
        inaktivert.dato shouldBe viderefoertOpphoer.dato
        inaktivert.vilkaar shouldBe viderefoertOpphoer.vilkaar
        inaktivert.kravdato shouldBe viderefoertOpphoer.kravdato
        inaktivert.skalViderefoere shouldBe viderefoertOpphoer.skalViderefoere
        inaktivert.begrunnelse shouldBe viderefoertOpphoer.begrunnelse
    }

    @Test
    fun `lagrer aktiv og inaktive opphoer paa samme behandling`() {
        val sak =
            SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() }).opprettSak(
                SOEKER_FOEDSELSNUMMER.value,
                SakType.BARNEPENSJON,
                Enhet.defaultEnhet,
            )
        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak.id)
        behandlingDao.opprettBehandling(behandling = opprettBehandling)

        service.oppdaterViderefoertOpphoer(
            behandlingId = opprettBehandling.id,
            viderefoertOpphoer =
                viderefoertOpphoer(
                    behandlingId = opprettBehandling.id,
                    opphoersdato = YearMonth.of(2024, Month.JANUARY),
                    skalViderefoere = JaNei.JA,
                ),
        )

        service.fjernViderefoertOpphoer(opprettBehandling.id, saksbehandlerKilde)

        service.oppdaterViderefoertOpphoer(
            behandlingId = opprettBehandling.id,
            viderefoertOpphoer =
                viderefoertOpphoer(
                    behandlingId = opprettBehandling.id,
                    opphoersdato = YearMonth.of(2024, Month.FEBRUARY),
                    skalViderefoere = JaNei.JA,
                ),
        )

        service.fjernViderefoertOpphoer(opprettBehandling.id, saksbehandlerKilde)

        service.oppdaterViderefoertOpphoer(
            behandlingId = opprettBehandling.id,
            viderefoertOpphoer =
                viderefoertOpphoer(
                    skalViderefoere = JaNei.JA,
                    behandlingId = opprettBehandling.id,
                    opphoersdato = YearMonth.of(2024, Month.MARCH),
                ),
        )

        val alleViderefoertOpphoer = hentAlleViderefoertOpphoer(opprettBehandling.id)

        alleViderefoertOpphoer
            .filter { it.aktiv }
            .map { it.dato } shouldContainExactly listOf(YearMonth.of(2024, Month.MARCH))

        alleViderefoertOpphoer
            .filter { !it.aktiv }
            .map { it.dato } shouldContainExactly
            listOf(
                YearMonth.of(2024, Month.JANUARY),
                YearMonth.of(2024, Month.FEBRUARY),
            )
    }

    @Test
    fun `feiler ved oppretting hvis det skal viderefoeres og vilkaar mangler`() {
        val sak =
            SakSkrivDao(SakendringerDao(ConnectionAutoclosingTest(dataSource)) { mockk() }).opprettSak(
                SOEKER_FOEDSELSNUMMER.value,
                SakType.BARNEPENSJON,
                Enhet.defaultEnhet,
            )
        val opprettBehandling = opprettBehandling(type = BehandlingType.FØRSTEGANGSBEHANDLING, sakId = sak.id)
        behandlingDao.opprettBehandling(behandling = opprettBehandling)

        shouldThrow<UgyldigForespoerselException> {
            service.oppdaterViderefoertOpphoer(
                behandlingId = opprettBehandling.id,
                viderefoertOpphoer =
                    viderefoertOpphoer(
                        opprettBehandling.id,
                        YearMonth.of(2024, Month.JANUARY),
                        skalViderefoere = JaNei.JA,
                        vilkaar = null,
                    ),
            )
        }
    }

    private fun viderefoertOpphoer(
        behandlingId: UUID,
        opphoersdato: YearMonth,
        skalViderefoere: JaNei,
        vilkaar: VilkaarType? = VilkaarType.BP_FORMAAL_2024,
    ): ViderefoertOpphoer =
        ViderefoertOpphoer(
            skalViderefoere = skalViderefoere,
            behandlingId = behandlingId,
            dato = opphoersdato,
            begrunnelse = "for testformål",
            vilkaar = vilkaar,
            kilde = saksbehandlerKilde,
            kravdato = null,
        )

    private fun hentAlleViderefoertOpphoer(behandlingId: UUID): List<ViderefoertOpphoer> =
        connection.hentConnection {
            with(it) {
                val statement =
                    prepareStatement(
                        "SELECT skalViderefoere, dato, kilde, begrunnelse, kravdato, vilkaar, aktiv " +
                            "FROM viderefoert_opphoer " +
                            "WHERE behandling_id = ? ",
                    )
                statement.setObject(1, behandlingId)
                statement.executeQuery().toList {
                    ViderefoertOpphoer(
                        skalViderefoere =
                            no.nav.etterlatte.libs.common.behandling.JaNei
                                .valueOf(getString("skalViderefoere")),
                        dato = getString("dato").let { objectMapper.readValue<YearMonth>(it) },
                        kilde = getString("kilde").let { objectMapper.readValue(it) },
                        begrunnelse = getString("begrunnelse"),
                        kravdato = getDate("kravdato")?.toLocalDate(),
                        behandlingId = behandlingId,
                        vilkaar = getString("vilkaar")?.let { VilkaarType.valueOf(it) },
                        aktiv = getBoolean("aktiv"),
                    )
                }
            }
        }
}
