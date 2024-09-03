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
    params: List<SQLParameter>,
    konverterer: (ResultSet.(Any?) -> T),
): List<T> =
    hentConnection {
        with(it) {
            val stmt = prepareStatement(statement.trimMargin())
            params.forEachIndexed { index, param -> param.settParameter(index + 1, stmt) }
            stmt.executeQuery()
        }.toList { konverterer(it) }
    }

fun ConnectionAutoclosing.opprett(
    statement: String,
    params: List<SQLParameter>,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.forEachIndexed { index, param -> param.settParameter(index + 1, stmt) }
        stmt.executeUpdate().also { require(it == 1) }
    }
}

fun <T> ConnectionAutoclosing.opprettOgReturner(
    statement: String,
    params: List<SQLParameter>,
    konverterer: ResultSet.(Any?) -> T,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.forEachIndexed { index, param -> param.settParameter(index + 1, stmt) }
        stmt.executeQuery().single { konverterer(it) }
    }
}

fun ConnectionAutoclosing.oppdater(
    statement: String,
    params: List<SQLParameter>,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.forEachIndexed { index, param -> param.settParameter(index + 1, stmt) }
        stmt.executeUpdate()
    }
}

fun <T> ConnectionAutoclosing.oppdaterOgReturner(
    statement: String,
    params: List<SQLParameter>,
    konverterer: ResultSet.(Any?) -> T,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.forEachIndexed { index, param -> param.settParameter(index + 1, stmt) }
        stmt.executeQuery().single { konverterer(it) }
    }
}

fun ConnectionAutoclosing.slett(
    statement: String,
    params: List<SQLParameter>,
) = hentConnection {
    with(it) {
        val stmt = prepareStatement(statement.trimMargin())
        params.forEachIndexed { index, param -> param.settParameter(index + 1, stmt) }
        stmt.executeUpdate()
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
