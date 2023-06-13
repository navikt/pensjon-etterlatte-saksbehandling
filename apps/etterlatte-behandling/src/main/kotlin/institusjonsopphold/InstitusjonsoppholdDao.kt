package no.nav.etterlatte.institusjonsopphold

import java.sql.Connection
import java.util.*

class InstitusjonsoppholdDao(private val connection: () -> Connection) {
    fun lagreInstitusjonsopphold(sakId: Long, institusjonoppholdBegrunnelse: InstitusjonsoppholdBegrunnelse) {
        with(connection()) {
            val statement = prepareStatement(
                "INSERT INTO institusjonsoppholdhendelse(id, sak_id, kanGiReduksjon, kanGiReduksjonTekst" +
                    "merEnnTreMaaneder, merEnnTreMaanederTekst) " +
                    "VALUES(?, ?, ?, ?, ?, ?)"
            )
            statement.setString(1, UUID.randomUUID().toString())
            statement.setLong(2, sakId)
            statement.setString(3, institusjonoppholdBegrunnelse.kanGiReduksjonAvYtelse.toString())
            statement.setString(4, institusjonoppholdBegrunnelse.kanGiReduksjonAvYtelseBegrunnelse)
            statement.setString(5, institusjonoppholdBegrunnelse.forventetVarighetMerEnn3Maaneder.toString())
            statement.setString(6, institusjonoppholdBegrunnelse.forventetVarighetMerEnn3MaanederBegrunnelse)
            statement.executeQuery()
        }
    }
}