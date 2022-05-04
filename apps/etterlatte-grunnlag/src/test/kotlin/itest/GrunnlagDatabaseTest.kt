package no.nav.etterlatte.itest

import no.nav.etterlatte.DataSourceBuilder
import no.nav.etterlatte.grunnlag.GrunnlagDao
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.grunnlag.objectMapper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.time.Instant
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GrunnlagDaoIntegrationTest {
    @Container
    private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:14")


    private lateinit var dataSource: DataSource

    //@BeforeAll
    fun beforeAll() {
        postgreSQLContainer.start()
        postgreSQLContainer.withUrlParam("user", postgreSQLContainer.username)
        postgreSQLContainer.withUrlParam("password", postgreSQLContainer.password)

        val dsb = DataSourceBuilder(mapOf("DB_JDBC_URL" to postgreSQLContainer.jdbcUrl))
        dataSource = dsb.dataSource

        dsb.migrate()
    }

    //@AfterAll
    fun afterAll() {
        postgreSQLContainer.stop()
    }

    //@Test
    fun `Legge til opplysning og hente den etterpå`() {
        val connection = dataSource.connection
        //val sakrepo = SakDao { connection }
        val grunnlagRepo = GrunnlagDao { connection }
        val opplysningRepo = OpplysningDao { connection }
        val deltOpplysning = Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also { opplysningRepo.nyOpplysning(it) }
        val ikkeDeltOpplysning = Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also { opplysningRepo.nyOpplysning(it) }
        //val sak1 = sakrepo.opprettSak("123", "BP").id
        //val sak2 = sakrepo.opprettSak("321", "BP").id
        //listOf(
          //  Grunnlag(UUID.randomUUID(), sak1, listOf(deltOpplysning)),
         //   Grunnlag(UUID.randomUUID(), sak1, listOf(ikkeDeltOpplysning)),
         //   Grunnlag(UUID.randomUUID(), sak2, listOf(deltOpplysning))
        //).forEach { b ->
         //   grunnlagRepo.opprett(b)
         //   b.grunnlag.forEach { o -> opplysningRepo.leggOpplysningTilGrunnlag(b.id, o.id) }
        //}

        //Assertions.assertEquals(2, grunnlagRepo.alleISak(sak1).size)


        //opplysningRepo.slettOpplysningerISak(sak1)
       //TODO dette er bare tull
        //grunnlagRepo.alleISak(sak1)

        //Assertions.assertEquals(0, grunnlagRepo.alleISak(sak1).size)
        //Assertions.assertEquals(1, grunnlagRepo.alleISak(sak2).size)
       // Assertions.assertEquals(
        //    1,
        //    opplysningRepo.finnOpplysningerIGrunnlag(grunnlagRepo.alleISak(sak2).first().saksId).size
        //)

        connection.close()
    }


    //@Test
    fun `avbryte sak`() {
        val connection = dataSource.connection
        //val sakrepo = SakDao { connection }
        val behandlingRepo = GrunnlagDao { connection }
        val opplysningRepo = OpplysningDao { connection }
        val ikkeDeltOpplysning = Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
            Opplysningstyper.SOEKNAD_MOTTATT_DATO,
            objectMapper.createObjectNode(),
            objectMapper.createObjectNode()
        ).also { opplysningRepo.nyOpplysning(it) }
        /*
        val sak1 = sakrepo.opprettSak("123", "BP").id
        listOf(
            Grunnlag(UUID.randomUUID(), sak1, listOf(ikkeDeltOpplysning)),
        ).forEach { b ->
            behandlingRepo.opprett(b)
            b.grunnlag.forEach { o -> opplysningRepo.leggOpplysningTilGrunnlag(b.id, o.id) }
        }

        Assertions.assertEquals(1, behandlingRepo.alleISak(sak1).size)

        var behandling = behandlingRepo.hentBehandlingerMedSakId(sak1)
        Assertions.assertEquals(1, behandling.size)
        //Assertions.assertEquals(false, behandling.first().avbrutt)

        //behandlingRepo.avbrytBehandling(behandling.first())
        behandling = behandlingRepo.hentBehandlingerMedSakId(sak1)
        Assertions.assertEquals(1, behandling.size)
        //Assertions.assertEquals(true, behandling.first().avbrutt)

        connection.close()

         */
    }

}
