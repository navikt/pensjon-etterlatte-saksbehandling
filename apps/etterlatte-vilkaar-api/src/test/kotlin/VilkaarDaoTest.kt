/*
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VilkaarDaoTest {

    private val postgres = PostgreSQLContainer<Nothing>("postgres:14")
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway
    private val behandlingId = UUID.randomUUID()

    @BeforeAll
    internal fun `start miljo`() {
        postgres.start()

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            initializationFailTimeout = 6000
            maximumPoolSize = 3
            maxLifetime = 30001

            validate()
        })
        flyway = Flyway.configure().dataSource(dataSource).load()
    }

    @BeforeEach
    internal fun setup() {
        flyway.clean()
        flyway.migrate()
        dataSource.lagreVilkaarsvurdering(behandlingId)
    }


    @AfterAll
    internal fun `stop embedded environment`() {
        postgres.stop()
    }

    private fun DataSource.lagreVilkaarsvurdering(


    ) {
        connection.use { connection ->
            val stmt = connection.prepareStatement(
                """INSERT INTO 
                    
                """.trimIndent()
            )
        }

    }


}
 */
