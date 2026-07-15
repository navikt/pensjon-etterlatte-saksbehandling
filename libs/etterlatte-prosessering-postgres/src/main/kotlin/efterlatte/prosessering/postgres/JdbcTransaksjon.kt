package efterlatte.prosessering.postgres

import efterlatte.prosessering.Transaksjon
import java.sql.Connection

/**
 * JDBC-bindingen av [Transaksjon]. Bærer kallerens [connection]. Repositoryet
 * pakker den ut og skriver på den, men committer eller lukker den **aldri** —
 * transaksjonen eies av den som åpnet den.
 */
class JdbcTransaksjon(
    internal val connection: Connection,
) : Transaksjon
