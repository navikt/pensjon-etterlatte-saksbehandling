package no.nav.etterlatte.institusjonsopphold

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.database.singleOrNull
import java.sql.Connection
import java.util.UUID

class InstitusjonsoppholdDao(private val connection: () -> Connection) {
    fun lagreInstitusjonsopphold(
        sakId: Long,
        saksbehandler: Grunnlagsopplysning.Saksbehandler,
        institusjonoppholdBegrunnelse: InstitusjonsoppholdBegrunnelse,
    ) {
        with(connection()) {
            val statement =
                prepareStatement(
                    "INSERT INTO institusjonsoppholdhendelse(id, sak_id, kanGiReduksjon, kanGiReduksjonTekst," +
                        "merEnnTreMaaneder, merEnnTreMaanederTekst, saksbehandler, grunnlagsendringshendelse_id) " +
                        "VALUES(?::UUID, ?, ?, ?, ?, ?, ?, ?::UUID)",
                )
            statement.setString(1, UUID.randomUUID().toString())
            statement.setLong(2, sakId)
            statement.setString(3, institusjonoppholdBegrunnelse.kanGiReduksjonAvYtelse.svar.toString())
            statement.setString(4, institusjonoppholdBegrunnelse.kanGiReduksjonAvYtelse.begrunnelse)
            statement.setString(5, institusjonoppholdBegrunnelse.forventetVarighetMerEnn3Maaneder.svar.toString())
            statement.setString(6, institusjonoppholdBegrunnelse.forventetVarighetMerEnn3Maaneder.begrunnelse)
            statement.setString(7, saksbehandler.toJson())
            statement.setString(8, institusjonoppholdBegrunnelse.grunnlagsEndringshendelseId)
            statement.executeUpdate()
        }
    }

    fun hentBegrunnelse(grunnlagsEndringshendelseId: String): InstitusjonsoppholdBegrunnelseMedSaksbehandler? {
        return with(connection()) {
            val statement =
                prepareStatement(
                    "SELECT * from institusjonsoppholdhendelse" +
                        " WHERE grunnlagsendringshendelse_id = ?::UUID",
                )
            statement.setString(1, grunnlagsEndringshendelseId)
            statement.executeQuery().singleOrNull {
                InstitusjonsoppholdBegrunnelseMedSaksbehandler(
                    kanGiReduksjonAvYtelse =
                        JaNeiMedBegrunnelse(
                            svar = JaNei.valueOf(getString("kanGiReduksjon")),
                            begrunnelse = getString("kanGiReduksjonTekst"),
                        ),
                    forventetVarighetMerEnn3Maaneder =
                        JaNeiMedBegrunnelse(
                            svar = JaNei.valueOf(getString("merEnnTreMaaneder")),
                            begrunnelse = getString("merEnnTreMaanederTekst"),
                        ),
                    grunnlagsEndringshendelseId = getString("grunnlagsendringshendelse_id"),
                    saksbehandler = getString("saksbehandler").let { objectMapper.readValue(it) },
                )
            }
        }
    }
}
