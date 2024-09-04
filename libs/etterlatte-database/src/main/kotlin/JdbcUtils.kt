package no.nav.etterlatte.libs.database

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.setTidspunkt
import org.postgresql.util.PGobject
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? =
    if (next()) {
        block().also {
            require(!next()) { "Skal være unik" }
        }
    } else {
        null
    }

fun <T> ResultSet.single(block: ResultSet.() -> T): T {
    require(next()) {
        "Skal ha en verdi"
    }
    return block().also {
        require(!next()) { "Skal være unik" }
    }
}

fun <T> ResultSet.firstOrNull(block: ResultSet.() -> T): T? =
    if (next()) {
        block()
    } else {
        null
    }

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
    val list = ArrayList<T>()
    while (next()) {
        list.add(block())
    }
    return list
}

inline fun <reified T : Any> PreparedStatement.setJsonb(
    parameterIndex: Int,
    jsonb: T?,
): PreparedStatement {
    if (jsonb == null) {
        this.setNull(parameterIndex, java.sql.Types.NULL)
    }
    val jsonObject = PGobject()
    jsonObject.type = "json"
    jsonObject.value = objectMapper.writeValueAsString(jsonb)
    this.setObject(parameterIndex, jsonObject)
    return this
}

fun <T> ConnectionAutoclosing.hent(
    statement: String,
    params: Map<Int, SQLParameter>,
    modus: Uthentingsmodus = Uthentingsmodus.SINGLE_OR_NULL,
    konverterer: (ResultSet.(Any?) -> T),
): T? =
    hentConnection {
        with(it) {
            val stmt = prepareStatement(statement.trimMargin())
            params.entries.forEach { entry -> entry.value.settParameter(entry.key, stmt) }
            stmt.executeQuery()
        }.let { modus.haandterUthenting(it, konverterer) }
    }

fun <T> ConnectionAutoclosing.hentListe(
    statement: String,
    params: Map<Int, SQLParameter>,
    konverterer: (ResultSet.(Any?) -> T),
): List<T> =
    hentConnection {
        with(it) {
            val stmt = prepareStatement(statement.trimMargin())
            params.entries.forEach { entry -> entry.value.settParameter(entry.key, stmt) }
            stmt.executeQuery()
        }.toList { konverterer(it) }
    }

fun ConnectionAutoclosing.opprett(
    statement: String,
    params: Map<Int, SQLParameter>,
    forventaResultat: ForventaResultat = ForventaResultat.IKKE_KJENT,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.entries.forEach { entry -> entry.value.settParameter(entry.key, stmt) }
        stmt.executeUpdate().also { forventaResultat.haandhevKrav(it) }
    }
}

fun <T> ConnectionAutoclosing.opprettOgReturner(
    statement: String,
    params: Map<Int, SQLParameter>,
    konverterer: ResultSet.(Any?) -> T,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.entries.forEach { entry -> entry.value.settParameter(entry.key, stmt) }
        stmt.executeQuery().single { konverterer(it) }
    }
}

fun ConnectionAutoclosing.oppdater(
    statement: String,
    params: Map<Int, SQLParameter>,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.entries.forEach { entry -> entry.value.settParameter(entry.key, stmt) }
        stmt.executeUpdate()
    }
}

fun <T> ConnectionAutoclosing.oppdaterOgReturner(
    statement: String,
    params: Map<Int, SQLParameter>,
    konverterer: ResultSet.(Any?) -> T,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.entries.forEach { entry -> entry.value.settParameter(entry.key, stmt) }
        stmt.executeQuery().single { konverterer(it) }
    }
}

fun ConnectionAutoclosing.slett(
    statement: String,
    params: Map<Int, SQLParameter>,
    forventaResultat: ForventaResultat = ForventaResultat.IKKE_KJENT,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.entries.forEach { entry -> entry.value.settParameter(entry.key, stmt) }
        stmt.executeUpdate().also { forventaResultat.haandhevKrav(it) }
    }
}

abstract class SQLParameter(
    open val verdi: Any?,
) {
    abstract fun settParameter(
        index: Int,
        stmt: PreparedStatement,
    ): Any?
}

data class SQLString(
    override val verdi: String?,
) : SQLParameter(verdi) {
    override fun settParameter(
        index: Int,
        stmt: PreparedStatement,
    ) = stmt.setString(index, verdi)
}

data class SQLInt(
    override val verdi: Int,
) : SQLParameter(verdi) {
    override fun settParameter(
        index: Int,
        stmt: PreparedStatement,
    ) = stmt.setInt(index, verdi)
}

data class SQLLong(
    override val verdi: Long,
) : SQLParameter(verdi) {
    override fun settParameter(
        index: Int,
        stmt: PreparedStatement,
    ) = stmt.setLong(index, verdi)
}

data class SQLDate(
    override val verdi: Date?,
) : SQLParameter(verdi) {
    override fun settParameter(
        index: Int,
        stmt: PreparedStatement,
    ) = stmt.setDate(index, verdi)
}

data class SQLObject(
    override val verdi: Any?,
) : SQLParameter(verdi) {
    override fun settParameter(
        index: Int,
        stmt: PreparedStatement,
    ) = stmt.setObject(index, verdi)
}

data class SQLJsonb(
    override val verdi: Any?,
) : SQLParameter(verdi) {
    override fun settParameter(
        index: Int,
        stmt: PreparedStatement,
    ) = stmt.setJsonb(index, verdi)
}

data class SQLTidspunkt(
    override val verdi: Tidspunkt?,
) : SQLParameter(verdi) {
    override fun settParameter(
        index: Int,
        stmt: PreparedStatement,
    ) = stmt.setTidspunkt(index, verdi)
}

enum class Uthentingsmodus {
    SINGLE_OR_NULL,
    FIRST_OR_NULL,
    ;

    internal fun <T> haandterUthenting(
        it: ResultSet,
        konverterer: ResultSet.(Any?) -> T,
    ): T? =
        when (this) {
            SINGLE_OR_NULL -> it.singleOrNull { konverterer(it) }
            FIRST_OR_NULL -> it.firstOrNull { konverterer(it) }
        }
}

/* Frå PreparedStatement-dokumentasjonen:
Returns:
either (1) the row count for SQL Data Manipulation Language (DML) statements
or (2) 0 for SQL statements that return nothing
 */
enum class ForventaResultat(
    val haandhevKrav: (verdi: Int) -> Unit,
) {
    RADER({ require(it == 1) }),
    INGEN_RADER({ require(it == 0) }),
    IKKE_KJENT({}),
}
