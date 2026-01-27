package no.nav.etterlatte.grunnlagsendring.doedshendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.database.setJsonb
import no.nav.etterlatte.libs.database.singleOrNull

class UkjentBeroertDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentUkjentBeroert(avdoedFnr: String): UkjentBeroert? =
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    SELECT avdoed_fnr, barn_uten_ident, ektefeller_uten_ident
                    FROM doedshendelse_ukjente_beroerte
                    WHERE avdoed_fnr = ?
                    """.trimIndent(),
                ).apply {
                    setString(1, avdoedFnr)
                }.executeQuery()
                    .singleOrNull {
                        UkjentBeroert(
                            avdoedFnr = getString("avdoed_fnr"),
                            barnUtenIdent = objectMapper.readValue<List<PersonUtenIdent>>(getString("barn_uten_ident")),
                            ektefellerUtenIdent = objectMapper.readValue<List<Sivilstand>>(getString("ektefeller_uten_ident")),
                        )
                    }
            }
        }

    fun lagreUkjentBeroert(ukjentBeroert: UkjentBeroert) {
        connectionAutoclosing.hentConnection {
            with(it) {
                prepareStatement(
                    """
                    INSERT INTO doedshendelse_ukjente_beroerte(avdoed_fnr, barn_uten_ident, ektefeller_uten_ident) 
                    VALUES (?, ?, ?)
                    ON CONFLICT (avdoed_fnr) DO UPDATE SET
                        barn_uten_ident = excluded.barn_uten_ident, 
                        ektefeller_uten_ident = excluded.ektefeller_uten_ident
                    """.trimIndent(),
                ).apply {
                    setString(1, ukjentBeroert.avdoedFnr)
                    setJsonb(2, ukjentBeroert.barnUtenIdent)
                    setJsonb(3, ukjentBeroert.ektefellerUtenIdent)
                }.executeUpdate()
            }
        }
    }
}
