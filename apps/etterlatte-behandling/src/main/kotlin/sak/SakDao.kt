package no.nav.etterlatte.sak

import no.nav.etterlatte.database.singleOrNull
import no.nav.etterlatte.database.toList
import java.sql.Connection



class SakDao(private val connection: ()->Connection) {

    fun hentSak(id: Long): Sak?{
        val statemant = connection().prepareStatement("SELECT id, sakType, fnr from sak where id = ?")
        statemant.setLong(1, id)
        return statemant.executeQuery().singleOrNull { Sak(
            sakType = getString(2),
            ident = getString(3),
            id = getLong(1)) }


    }
    fun opprettSak(fnr: String, type: String): Sak{
        val statement = connection().prepareStatement("INSERT INTO sak(sakType, fnr) VALUES(?, ?) RETURNING id, sakType, fnr")
        statement.setString(1, type)
        statement.setString(2, fnr)
        return  requireNotNull(statement.executeQuery().singleOrNull { Sak(
            sakType = getString(2),
            ident = getString(3),
            id = getLong(1)) })
    }

    fun finnSaker(fnr: String): List<Sak>{
        val statemant = connection().prepareStatement("SELECT id, sakType, fnr from sak where fnr = ?")
        statemant.setString(1, fnr)
        return statemant.executeQuery().toList { Sak(
            sakType = getString(2),
            ident = getString(3),
            id = getLong(1)) }
    }


}


