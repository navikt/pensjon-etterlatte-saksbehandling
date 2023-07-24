package no.nav.etterlatte.itest

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.verify
import lagGrunnlagsopplysning
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_SOEKNAD_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKNAD_MOTTATT_DATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.testdata.grunnlag.ADRESSE_DEFAULT
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")

    private lateinit var opplysningRepo: OpplysningDao
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        dataSource = DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password
        )
        dataSource.migrate()

        opplysningRepo = spyk(OpplysningDao(dataSource))
    }

    @AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
        dataSource.connection.use {
            it.prepareStatement(""" TRUNCATE grunnlagshendelse""").execute()
        }
    }

    @Test
    fun `Legge til opplysning og hente den etterpaa`() {
        val datoMottat = LocalDate.of(2022, Month.MAY, 3).atStartOfDay()
        val uuid = UUID.randomUUID()

        lagGrunnlagsopplysning(
            opplysningstype = SOEKNAD_MOTTATT_DATO,
            verdi = objectMapper.valueToTree(SoeknadMottattDato(datoMottat)) as ObjectNode
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        lagGrunnlagsopplysning(
            opplysningstype = SOEKNAD_MOTTATT_DATO,
            uuid = uuid
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(1).size)
        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(2).size)
        assertEquals(uuid, opplysningRepo.finnHendelserIGrunnlag(2).first().opplysning.id)
        assertEquals(
            datoMottat,
            opplysningRepo.finnHendelserIGrunnlag(1)
                .first().opplysning.let { objectMapper.treeToValue<SoeknadMottattDato>(it.opplysning) }.mottattDato
        )
    }

    @Test
    fun `kan legge til en personopplysning`() {
        val uuid = UUID.randomUUID()
        val fnr = Folkeregisteridentifikator.of("13082819155")

        lagGrunnlagsopplysning(
            uuid = uuid,
            opplysningstype = Opplysningstype.NAVN,
            verdi = objectMapper.valueToTree("Per"),
            fnr = fnr
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it, fnr) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(1).size)
        assertEquals(uuid, opplysningRepo.finnHendelserIGrunnlag(1).first().opplysning.id)
        assertEquals(
            fnr,
            opplysningRepo.finnHendelserIGrunnlag(1).first().opplysning.fnr
        )
    }

    @Test
    fun `skal liste alle nyeste opplysningene knyttet til en sak`() {
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        lagGrunnlagsopplysning(SOEKER_PDL_V1).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(1).size)
        assertEquals(2, opplysningRepo.finnHendelserIGrunnlag(2).size)
    }

    @Test
    fun `Skal hente grunnlag opptil versjon på sak`() {
        lagGrunnlagsopplysning(Opplysningstype.FOEDSELSDATO).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it) }
        lagGrunnlagsopplysning(Opplysningstype.DOEDSDATO).also { opplysningRepo.leggOpplysningTilGrunnlag(1, it) }

        assertEquals(1, opplysningRepo.finnGrunnlagOpptilVersjon(1, 1).size)
        assertEquals(2, opplysningRepo.finnGrunnlagOpptilVersjon(1, 2).size)
    }

    @Test
    fun `Skal hente opplysning fra nyeste hendelse basert paa sakId og opplysningType`() {
        val uuid = UUID.randomUUID()

        lagGrunnlagsopplysning(AVDOED_PDL_V1).also { opplysningRepo.leggOpplysningTilGrunnlag(33, it) }
        lagGrunnlagsopplysning(AVDOED_PDL_V1, uuid = uuid).also { opplysningRepo.leggOpplysningTilGrunnlag(33, it) }

        assertEquals(uuid, opplysningRepo.finnNyesteGrunnlag(33, Opplysningstype.AVDOED_PDL_V1)?.opplysning?.id)
        // Skal håndtere at opplysning ikke finnes
        assertEquals(null, opplysningRepo.finnNyesteGrunnlag(0L, Opplysningstype.AVDOED_PDL_V1))
    }

    @Test
    fun `Ny opplysning skal overskrive gammel, new way`() {
        val uuid = UUID.randomUUID()

        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(2, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO, uuid = uuid).also {
            opplysningRepo.leggOpplysningTilGrunnlag(
                2,
                it
            )
        }

        opplysningRepo.finnHendelserIGrunnlag(2).also {
            assertEquals(1, it.size)
            assertEquals(uuid, it.first().opplysning.id)
        }
    }

    @Test
    fun `kan lage og hente periodiserte opplysninger`() {
        val opplysning = Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = kilde,
            opplysningType = BOSTEDSADRESSE,
            meta = objectMapper.createObjectNode(),
            opplysning = ADRESSE_DEFAULT.first().toJsonNode(),
            attestering = null,
            fnr = SOEKER_FOEDSELSNUMMER,
            periode = Periode(
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 12)
            )
        )

        opplysningRepo.leggOpplysningTilGrunnlag(1, opplysning, SOEKER_FOEDSELSNUMMER)
        val expected = OpplysningDao.GrunnlagHendelse(opplysning, 1, 1).toJson()
        val actual = opplysningRepo.hentAlleGrunnlagForSak(1).first().toJson()

        assertEquals(expected, actual)
    }

    @Test
    fun `Finn alle persongalleri person er tilknyttet`() {
        val gjenlevendeFnr = TRIVIELL_MIDTPUNKT

        val barnepensjonSoeker1 = BLAAOEYD_SAKS
        val persongalleri1 = Persongalleri(
            soeker = barnepensjonSoeker1.value,
            innsender = gjenlevendeFnr.value,
            soesken = listOf(
                GOEYAL_KRONJUVEL.value,
                GROENN_STAUDE.value
            ),
            avdoed = listOf(STOR_SNERK.value),
            gjenlevende = listOf(gjenlevendeFnr.value)
        )

        val opplysning1 =
            lagGrunnlagsopplysning(PERSONGALLERI_V1, verdi = persongalleri1.toJsonNode(), fnr = barnepensjonSoeker1)
        opplysningRepo.leggOpplysningTilGrunnlag(1, opplysning1, barnepensjonSoeker1)

        val barnepensjonSoeker2 = GOEYAL_KRONJUVEL
        val persongalleri2 = Persongalleri(
            soeker = barnepensjonSoeker2.value,
            innsender = gjenlevendeFnr.value,
            soesken = listOf(
                BLAAOEYD_SAKS.value,
                GROENN_STAUDE.value
            ),
            avdoed = listOf(STOR_SNERK.value),
            gjenlevende = listOf(gjenlevendeFnr.value)
        )

        val opplysning2 =
            lagGrunnlagsopplysning(PERSONGALLERI_V1, verdi = persongalleri2.toJsonNode(), fnr = barnepensjonSoeker2)
        opplysningRepo.leggOpplysningTilGrunnlag(2, opplysning2, barnepensjonSoeker2)

        val persongalleri3 = Persongalleri(
            soeker = gjenlevendeFnr.value,
            innsender = gjenlevendeFnr.value,
            avdoed = listOf(STOR_SNERK.value)
        )

        val opplysning3 =
            lagGrunnlagsopplysning(PERSONGALLERI_V1, verdi = persongalleri3.toJsonNode(), fnr = gjenlevendeFnr)
        opplysningRepo.leggOpplysningTilGrunnlag(3, opplysning3, gjenlevendeFnr)

        // gjenlevende skal finnes i 3 behandlingshendelser
        assertEquals(3, opplysningRepo.finnAllePersongalleriHvorPersonFinnes(gjenlevendeFnr).size)
        // BP soeker 1 skal finnes i 2 behandlingshendelser
        assertEquals(2, opplysningRepo.finnAllePersongalleriHvorPersonFinnes(barnepensjonSoeker1).size)
        // BP soeker 2 skal finnes i 2 behandlingshendelser
        assertEquals(2, opplysningRepo.finnAllePersongalleriHvorPersonFinnes(barnepensjonSoeker2).size)
        // Søsken GROENN_STAUDE har ikke søkt, men skal finnes i 2 behandlingshendelser
        assertEquals(2, opplysningRepo.finnAllePersongalleriHvorPersonFinnes(GROENN_STAUDE).size)

        assertTrue(opplysningRepo.finnAllePersongalleriHvorPersonFinnes(GROENN_KOPP).isEmpty())
        assertTrue(opplysningRepo.finnAllePersongalleriHvorPersonFinnes(SMEKKER_GYNGEHEST).isEmpty())

        verify(exactly = 1) { opplysningRepo.leggOpplysningTilGrunnlag(1, opplysning1, barnepensjonSoeker1) }
        verify(exactly = 1) { opplysningRepo.leggOpplysningTilGrunnlag(2, opplysning2, barnepensjonSoeker2) }
        verify(exactly = 1) { opplysningRepo.leggOpplysningTilGrunnlag(3, opplysning3, gjenlevendeFnr) }
    }

    @Test
    fun `Uthenting av alle saker tilknyttet person fungerer`() {
        val grunnlagsopplysning1 = lagGrunnlagsopplysning(AVDOED_PDL_V1, fnr = STOR_SNERK)
        opplysningRepo.leggOpplysningTilGrunnlag(1, grunnlagsopplysning1, STOR_SNERK)

        val grunnlagsopplysning2 = lagGrunnlagsopplysning(
            PERSONGALLERI_V1,
            verdi = Persongalleri(
                soeker = BLAAOEYD_SAKS.value,
                gjenlevende = listOf(GROENN_KOPP.value),
                avdoed = listOf(STOR_SNERK.value)
            ).toJsonNode()
        )
        opplysningRepo.leggOpplysningTilGrunnlag(2, grunnlagsopplysning2)

        val grunnlagsopplysning3 = lagGrunnlagsopplysning(
            PERSONGALLERI_V1,
            verdi = Persongalleri(
                soeker = BLAAOEYD_SAKS.value,
                gjenlevende = emptyList(),
                avdoed = listOf(STOR_SNERK.value, GROENN_KOPP.value)
            ).toJsonNode()
        )
        opplysningRepo.leggOpplysningTilGrunnlag(3, grunnlagsopplysning3)

        // mange dummy-opplysninger tilknyttet andre personer, skal ignoreres...
        listOf(BLAAOEYD_SAKS, GROENN_KOPP, TRIVIELL_MIDTPUNKT, SMEKKER_GYNGEHEST).forEachIndexed { i, fnr ->
            opplysningRepo.leggOpplysningTilGrunnlag(i.toLong(), lagGrunnlagsopplysning(SOEKER_SOEKNAD_V1, fnr = fnr))
            opplysningRepo.leggOpplysningTilGrunnlag(i.toLong(), lagGrunnlagsopplysning(AVDOED_PDL_V1, fnr = fnr))
            opplysningRepo.leggOpplysningTilGrunnlag(i.toLong(), lagGrunnlagsopplysning(PERSONGALLERI_V1, fnr = fnr))
        }

        val result = opplysningRepo.finnAlleSakerForPerson(STOR_SNERK)
        assertEquals(3, result.size)

        verify(exactly = 15) { opplysningRepo.leggOpplysningTilGrunnlag(any(), any(), any()) }
    }

    private companion object {
        val TRIVIELL_MIDTPUNKT = Folkeregisteridentifikator.of("19040550081")
        val STOR_SNERK = Folkeregisteridentifikator.of("11057523044")
        val GROENN_KOPP = Folkeregisteridentifikator.of("29018322402")
        val SMEKKER_GYNGEHEST = Folkeregisteridentifikator.of("11078431921")

        // barn
        val BLAAOEYD_SAKS = Folkeregisteridentifikator.of("05111850870")
        val GOEYAL_KRONJUVEL = Folkeregisteridentifikator.of("27121779531")
        val GROENN_STAUDE = Folkeregisteridentifikator.of("09011350027")
    }
}