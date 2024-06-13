package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toTimestamp
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.FaktiskTrygdetid
import no.nav.etterlatte.libs.common.trygdetid.FremtidigTrygdetid
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.tidspunkt
import no.nav.etterlatte.libs.database.transaction
import java.time.Period
import java.util.UUID
import javax.sql.DataSource

class TrygdetidRepository(
    private val dataSource: DataSource,
) {
    fun hentTrygdetidMedId(
        behandlingId: UUID,
        trygdetidId: UUID,
    ): Trygdetid? = hentTrygdetiderForBehandling(behandlingId).find { it.id == trygdetidId }

    fun hentTrygdetid(behandlingId: UUID): Trygdetid? = hentTrygdetiderForBehandling(behandlingId).minByOrNull { it.ident }

    fun hentTrygdetiderForBehandling(behandlingId: UUID): List<Trygdetid> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    SELECT
                        id,
                        sak_id,
                        behandling_id,
                        ident,
                        tidspunkt,
                        faktisk_trygdetid_norge_total,
                        faktisk_trygdetid_norge_antall_maaneder,
                        faktisk_trygdetid_teoretisk_total,
                        faktisk_trygdetid_teoretisk_antall_maaneder,
                        fremtidig_trygdetid_norge_total,
                        fremtidig_trygdetid_norge_antall_maaneder,
                        fremtidig_trygdetid_norge_opptjeningstid_maaneder,
                        fremtidig_trygdetid_norge_mindre_enn_fire_femtedeler,
                        fremtidig_trygdetid_teoretisk_total,
                        fremtidig_trygdetid_teoretisk_antall_maaneder,
                        fremtidig_trygdetid_teoretisk_opptjeningstid_maaneder,
                        fremtidig_trygdetid_teoretisk_mindre_enn_fire_femtedeler,
                        samlet_trygdetid_norge,
                        samlet_trygdetid_teoretisk,
                        prorata_broek_teller,
                        prorata_broek_nevner,
                        trygdetid_tidspunkt,
                        trygdetid_regelresultat,
                        beregnet_trygdetid_overstyrt,
                        poengaar_overstyrt,
                        yrkesskade,
                        beregnet_samlet_trygdetid_norge
                    FROM trygdetid 
                    WHERE behandling_id = :behandlingId
                    """.trimIndent(),
                paramMap = mapOf("behandlingId" to behandlingId),
            ).let { query ->
                session.run(
                    query
                        .map { row ->
                            val trygdetidId = row.uuid("id")
                            val trygdetidGrunnlag = hentTrygdetidGrunnlag(trygdetidId)
                            val opplysninger = hentGrunnlagOpplysninger(trygdetidId)
                            row.toTrygdetid(trygdetidGrunnlag, opplysninger)
                        }.asList,
                )
            }
        }

    fun opprettTrygdetid(trygdetid: Trygdetid): Trygdetid =
        dataSource
            .transaction { tx ->
                opprettTrygdetid(trygdetid, tx)
                opprettOpplysningsgrunnlag(trygdetid.id, trygdetid.opplysninger, tx)
                trygdetid.trygdetidGrunnlag.forEach { opprettTrygdetidGrunnlag(trygdetid.id, it, tx) }

                if (trygdetid.beregnetTrygdetid != null) {
                    oppdaterBeregnetTrygdetid(trygdetid.behandlingId, trygdetid.id, trygdetid.beregnetTrygdetid, tx)
                }
            }.let { hentTrygdetidMedIdNotNull(behandlingId = trygdetid.behandlingId, trygdetidId = trygdetid.id) }

    fun oppdaterTrygdetid(
        oppdatertTrygdetid: Trygdetid,
        overstyrt: Boolean = false,
    ): Trygdetid =
        dataSource
            .transaction { tx ->
                val gjeldendeTrygdetid =
                    hentTrygdetidMedIdNotNull(
                        behandlingId = oppdatertTrygdetid.behandlingId,
                        trygdetidId = oppdatertTrygdetid.id,
                    )

                oppdaterOverstyrtPoengaar(
                    gjeldendeTrygdetid.id,
                    gjeldendeTrygdetid.behandlingId,
                    oppdatertTrygdetid.overstyrtNorskPoengaar,
                    tx,
                )

                oppdaterYrkesskade(
                    gjeldendeTrygdetid.id,
                    gjeldendeTrygdetid.behandlingId,
                    oppdatertTrygdetid.yrkesskade,
                    tx,
                )

                // opprett grunnlag
                oppdatertTrygdetid.trygdetidGrunnlag
                    .filter { gjeldendeTrygdetid.trygdetidGrunnlag.find { tg -> tg.id == it.id } == null }
                    .forEach { opprettTrygdetidGrunnlag(oppdatertTrygdetid.id, it, tx) }

                // oppdater grunnlag
                oppdatertTrygdetid.trygdetidGrunnlag.forEach { trygdetidGrunnlag ->
                    gjeldendeTrygdetid.trygdetidGrunnlag
                        .find { it.id == trygdetidGrunnlag.id && it != trygdetidGrunnlag }
                        ?.let { oppdaterTrygdetidGrunnlag(trygdetidGrunnlag, tx) }
                }

                // slett grunnlag
                gjeldendeTrygdetid.trygdetidGrunnlag
                    .filter { oppdatertTrygdetid.trygdetidGrunnlag.find { tg -> tg.id == it.id } == null }
                    .forEach { slettTrygdetidGrunnlag(it.id, tx) }

                // overskriv opplysningsgrunnlag
                oppdaterOpplysningsgrunnlag(oppdatertTrygdetid.id, oppdatertTrygdetid.opplysninger, tx)

                if (oppdatertTrygdetid.beregnetTrygdetid != null) {
                    oppdaterBeregnetTrygdetid(
                        oppdatertTrygdetid.behandlingId,
                        oppdatertTrygdetid.id,
                        oppdatertTrygdetid.beregnetTrygdetid,
                        tx,
                        overstyrt,
                    )
                } else {
                    nullstillBeregnetTrygdetid(oppdatertTrygdetid.behandlingId, tx)
                }
            }.let { hentTrygdetidMedIdNotNull(oppdatertTrygdetid.behandlingId, oppdatertTrygdetid.id) }

    fun slettTrygdetid(trygdetidId: UUID) =
        dataSource.transaction { tx ->
            queryOf(
                statement =
                    """
                    DELETE FROM trygdetid CASCADE WHERE id = :id
                    """.trimIndent(),
                paramMap =
                    mapOf(
                        "id" to trygdetidId,
                    ),
            ).let { query ->
                tx.update(query)
            }
        }

    private fun opprettTrygdetid(
        trygdetid: Trygdetid,
        tx: TransactionalSession,
    ) = queryOf(
        statement =
            """
            INSERT INTO trygdetid(id, behandling_id, sak_id, ident, yrkesskade) VALUES(:id, :behandlingId, :sakId, :ident, :yrkesskade)
            """.trimIndent(),
        paramMap =
            mapOf(
                "id" to trygdetid.id,
                "behandlingId" to trygdetid.behandlingId,
                "sakId" to trygdetid.sakId,
                "ident" to trygdetid.ident,
                "yrkesskade" to trygdetid.yrkesskade,
            ),
    ).let { query -> tx.update(query) }

    private fun opprettOpplysningsgrunnlag(
        trygdetidId: UUID?,
        opplysninger: List<Opplysningsgrunnlag>,
        tx: TransactionalSession,
    ) = opplysninger.forEach { opplysningsgrunnlag ->
        queryOf(
            statement =
                """
                INSERT INTO opplysningsgrunnlag(id, trygdetid_id, type, opplysning, kilde)
                 VALUES(:id, :trygdetidId, :type, :opplysning::JSONB, :kilde::JSONB)
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to UUID.randomUUID(),
                    "trygdetidId" to trygdetidId,
                    "type" to opplysningsgrunnlag.type.name,
                    "opplysning" to opplysningsgrunnlag.opplysning.toJson(),
                    "kilde" to opplysningsgrunnlag.kilde.toJson(),
                ),
        ).let { query -> tx.update(query) }
    }

    private fun oppdaterOpplysningsgrunnlag(
        trygdetidId: UUID?,
        opplysninger: List<Opplysningsgrunnlag>,
        tx: TransactionalSession,
    ) {
        queryOf(
            statement =
                """
                DELETE FROM opplysningsgrunnlag
                WHERE trygdetid_id = :trygdetidId
                """.trimIndent(),
            paramMap =
                mapOf(
                    "trygdetidId" to trygdetidId,
                ),
        ).let { query -> tx.update(query) }

        opprettOpplysningsgrunnlag(trygdetidId, opplysninger, tx)
    }

    private fun opprettTrygdetidGrunnlag(
        trygdetidId: UUID,
        trygdetidGrunnlag: TrygdetidGrunnlag,
        tx: TransactionalSession,
    ) {
        queryOf(
            statement =
                """
                INSERT INTO trygdetid_grunnlag(
                    id, 
                    trygdetid_id, 
                    type, bosted, 
                    periode_fra, 
                    periode_til,
                    kilde,
                    beregnet_verdi, 
                    beregnet_tidspunkt, 
                    beregnet_regelresultat,
                    begrunnelse,
                    poeng_inn_aar,
                    poeng_ut_aar,
                    prorata
                ) 
                VALUES(:id, :trygdetidId, :type, :bosted, :periodeFra, :periodeTil, :kilde, 
                    :beregnetVerdi, :beregnetTidspunkt, :beregnetRegelresultat, :begrunnelse,
                    :poengInnAar, :poengUtAar, :prorata)
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to trygdetidGrunnlag.id,
                    "trygdetidId" to trygdetidId,
                    "type" to trygdetidGrunnlag.type.name,
                    "bosted" to trygdetidGrunnlag.bosted,
                    "periodeFra" to trygdetidGrunnlag.periode.fra,
                    "periodeTil" to trygdetidGrunnlag.periode.til,
                    "kilde" to trygdetidGrunnlag.kilde.toJson(),
                    "beregnetVerdi" to trygdetidGrunnlag.beregnetTrygdetid?.verdi?.toString(),
                    "beregnetTidspunkt" to trygdetidGrunnlag.beregnetTrygdetid?.tidspunkt?.toTimestamp(),
                    "beregnetRegelresultat" to trygdetidGrunnlag.beregnetTrygdetid?.regelResultat?.toJson(),
                    "begrunnelse" to trygdetidGrunnlag.begrunnelse,
                    "poengInnAar" to trygdetidGrunnlag.poengInnAar,
                    "poengUtAar" to trygdetidGrunnlag.poengUtAar,
                    "prorata" to trygdetidGrunnlag.prorata,
                ),
        ).let { query -> tx.update(query) }
    }

    private fun oppdaterTrygdetidGrunnlag(
        trygdetidGrunnlag: TrygdetidGrunnlag,
        tx: TransactionalSession,
    ) {
        queryOf(
            statement =
                """
                UPDATE trygdetid_grunnlag
                SET bosted = :bosted,
                 periode_fra = :periodeFra,
                 periode_til = :periodeTil,
                 kilde = :kilde,
                 beregnet_verdi = :beregnetVerdi, 
                 beregnet_tidspunkt = :beregnetTidspunkt, 
                 beregnet_regelresultat = :beregnetRegelresultat,
                 begrunnelse = :begrunnelse,
                 poeng_inn_aar = :poengInnAar,
                 poeng_ut_aar = :poengUtAar,
                 prorata = :prorata
                WHERE id = :trygdetidGrunnlagId
                """.trimIndent(),
            paramMap =
                mapOf(
                    "trygdetidGrunnlagId" to trygdetidGrunnlag.id,
                    "bosted" to trygdetidGrunnlag.bosted,
                    "periodeFra" to trygdetidGrunnlag.periode.fra,
                    "periodeTil" to trygdetidGrunnlag.periode.til,
                    "kilde" to trygdetidGrunnlag.kilde.toJson(),
                    "beregnetVerdi" to trygdetidGrunnlag.beregnetTrygdetid?.verdi?.toString(),
                    "beregnetTidspunkt" to trygdetidGrunnlag.beregnetTrygdetid?.tidspunkt?.toTimestamp(),
                    "beregnetRegelresultat" to trygdetidGrunnlag.beregnetTrygdetid?.regelResultat?.toJson(),
                    "begrunnelse" to trygdetidGrunnlag.begrunnelse,
                    "poengInnAar" to trygdetidGrunnlag.poengInnAar,
                    "poengUtAar" to trygdetidGrunnlag.poengUtAar,
                    "prorata" to trygdetidGrunnlag.prorata,
                ),
        ).let { query -> tx.update(query) }
    }

    private fun slettTrygdetidGrunnlag(
        trygdetidGrunnlagId: UUID,
        tx: TransactionalSession,
    ) {
        queryOf(
            statement = "DELETE FROM trygdetid_grunnlag WHERE id = :id",
            paramMap =
                mapOf(
                    "id" to trygdetidGrunnlagId,
                ),
        ).let { query -> tx.update(query) }
    }

    private fun oppdaterOverstyrtPoengaar(
        id: UUID,
        behandlingId: UUID,
        overstyrtNorskPoengaar: Int? = null,
        tx: TransactionalSession,
    ) {
        queryOf(
            statement =
                """
                UPDATE trygdetid 
                  SET poengaar_overstyrt = :overstyrtNorskPoengaar WHERE id = :id AND  behandling_id = :behandlingId
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to id,
                    "behandlingId" to behandlingId,
                    "overstyrtNorskPoengaar" to overstyrtNorskPoengaar,
                ),
        ).let { query ->
            tx.update(query)
        }
    }

    private fun oppdaterYrkesskade(
        id: UUID,
        behandlingId: UUID,
        yrkesskade: Boolean,
        tx: TransactionalSession,
    ) {
        queryOf(
            statement =
                """
                UPDATE trygdetid 
                  SET yrkesskade = :yrkesskade WHERE id = :id AND  behandling_id = :behandlingId
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to id,
                    "behandlingId" to behandlingId,
                    "yrkesskade" to yrkesskade,
                ),
        ).let { query ->
            tx.update(query)
        }
    }

    private fun oppdaterBeregnetTrygdetid(
        behandlingId: UUID,
        trygdetidId: UUID,
        beregnetTrygdetid: DetaljertBeregnetTrygdetid,
        tx: TransactionalSession,
        overstyrt: Boolean = false,
    ) {
        val beregnetVerdi = beregnetTrygdetid.resultat

        queryOf(
            statement =
                """
                UPDATE trygdetid 
                SET
                  faktisk_trygdetid_norge_total = :faktiskTrygdetidNorgeTotal,
                  faktisk_trygdetid_norge_antall_maaneder = :faktiskTrygdetidNorgeAntallMaaneder,
                  faktisk_trygdetid_teoretisk_total = :faktiskTrygdetidTeoretiskTotal,
                  faktisk_trygdetid_teoretisk_antall_maaneder = :faktiskTrygdetidTeoretiskAntallMaaneder,
                  fremtidig_trygdetid_norge_total = :fremtidigTrygdetidNorgeTotal,
                  fremtidig_trygdetid_norge_antall_maaneder = :fremtidigTrygdetidNorgeAntallMaaneder,
                  fremtidig_trygdetid_norge_opptjeningstid_maaneder = :fremtidigTrygdetidNorgeOpptjeningstidMaaneder,
                  fremtidig_trygdetid_norge_mindre_enn_fire_femtedeler =
                    :fremtidigTrygdetidNorgeMindreEnnFireFemtedeler,
                  fremtidig_trygdetid_teoretisk_total = :fremtidigTrygdetidTeoretiskTotal,
                  fremtidig_trygdetid_teoretisk_antall_maaneder = :fremtidigTrygdetidTeoretiskAntallMaaneder,
                  fremtidig_trygdetid_teoretisk_opptjeningstid_maaneder =
                    :fremtidigTrygdetidTeoretiskOpptjeningstidMaaneder,
                  fremtidig_trygdetid_teoretisk_mindre_enn_fire_femtedeler =
                    :fremtidigTrygdetidTeoretiskMindreEnnFireFemtedeler,
                  samlet_trygdetid_norge = :samletTrygdetidNorge,
                  samlet_trygdetid_teoretisk = :samletTrygdetidTeoretisk,
                  prorata_broek_teller = :prorataBroekTeller,
                  prorata_broek_nevner = :prorataBroekNevner,
                  trygdetid_tidspunkt = :trygdetidTidspunkt,
                  trygdetid_regelresultat = :trygdetidRegelresultat,
                  beregnet_trygdetid_overstyrt = :overstyrt,
                  yrkesskade = :yrkesskade
                WHERE behandling_id = :behandlingId AND id = :trygdetidId
                """.trimIndent(),
            paramMap =
                mapOf(
                    "behandlingId" to behandlingId,
                    "trygdetidId" to trygdetidId,
                    "faktiskTrygdetidNorgeTotal" to beregnetVerdi.faktiskTrygdetidNorge?.periode?.toString(),
                    "faktiskTrygdetidNorgeAntallMaaneder" to beregnetVerdi.faktiskTrygdetidNorge?.antallMaaneder,
                    "faktiskTrygdetidTeoretiskTotal" to beregnetVerdi.faktiskTrygdetidTeoretisk?.periode?.toString(),
                    "faktiskTrygdetidTeoretiskAntallMaaneder" to
                        beregnetVerdi.faktiskTrygdetidTeoretisk?.antallMaaneder,
                    "fremtidigTrygdetidNorgeTotal" to beregnetVerdi.fremtidigTrygdetidNorge?.periode?.toString(),
                    "fremtidigTrygdetidNorgeAntallMaaneder" to beregnetVerdi.fremtidigTrygdetidNorge?.antallMaaneder,
                    "fremtidigTrygdetidNorgeOpptjeningstidMaaneder" to
                        beregnetVerdi.fremtidigTrygdetidNorge?.opptjeningstidIMaaneder,
                    "fremtidigTrygdetidNorgeMindreEnnFireFemtedeler" to
                        beregnetVerdi.fremtidigTrygdetidNorge?.mindreEnnFireFemtedelerAvOpptjeningstiden,
                    "fremtidigTrygdetidTeoretiskTotal" to
                        beregnetVerdi.fremtidigTrygdetidTeoretisk?.periode?.toString(),
                    "fremtidigTrygdetidTeoretiskAntallMaaneder" to
                        beregnetVerdi.fremtidigTrygdetidTeoretisk?.antallMaaneder,
                    "fremtidigTrygdetidTeoretiskOpptjeningstidMaaneder" to
                        beregnetVerdi.fremtidigTrygdetidTeoretisk?.opptjeningstidIMaaneder,
                    "fremtidigTrygdetidTeoretiskMindreEnnFireFemtedeler" to
                        beregnetVerdi.fremtidigTrygdetidTeoretisk?.mindreEnnFireFemtedelerAvOpptjeningstiden,
                    "samletTrygdetidNorge" to beregnetVerdi.samletTrygdetidNorge,
                    "samletTrygdetidTeoretisk" to beregnetVerdi.samletTrygdetidTeoretisk,
                    "prorataBroekTeller" to beregnetVerdi.prorataBroek?.teller,
                    "prorataBroekNevner" to beregnetVerdi.prorataBroek?.nevner,
                    "trygdetidTidspunkt" to beregnetTrygdetid.tidspunkt.toTimestamp(),
                    "trygdetidRegelresultat" to beregnetTrygdetid.regelResultat.toJson(),
                    "overstyrt" to overstyrt,
                    "yrkesskade" to beregnetVerdi.yrkesskade,
                ),
        ).let { query ->
            tx.update(query)
        }
    }

    private fun nullstillBeregnetTrygdetid(
        behandlingId: UUID,
        tx: TransactionalSession,
    ) = queryOf(
        statement =
            """
            UPDATE trygdetid 
            SET 
                faktisk_trygdetid_norge_total = null,
                faktisk_trygdetid_norge_antall_maaneder = null,
                faktisk_trygdetid_teoretisk_total = null,
                faktisk_trygdetid_teoretisk_antall_maaneder = null,
                fremtidig_trygdetid_norge_total = null,
                fremtidig_trygdetid_norge_antall_maaneder = null,
                fremtidig_trygdetid_norge_opptjeningstid_maaneder = null,
                fremtidig_trygdetid_norge_mindre_enn_fire_femtedeler = null,
                fremtidig_trygdetid_teoretisk_total =null,
                fremtidig_trygdetid_teoretisk_antall_maaneder = null,
                fremtidig_trygdetid_teoretisk_opptjeningstid_maaneder = null,
                fremtidig_trygdetid_teoretisk_mindre_enn_fire_femtedeler = null,
                samlet_trygdetid_norge = null,
                samlet_trygdetid_teoretisk = null,
                prorata_broek_teller = null,
                prorata_broek_nevner = null,
                trygdetid_tidspunkt = null,
                trygdetid_regelresultat = null,
                beregnet_trygdetid_overstyrt = false,
                yrkesskade = false
            WHERE behandling_id = :behandlingId
            """.trimIndent(),
        paramMap = mapOf("behandlingId" to behandlingId),
    ).let { query -> tx.update(query) }

    private fun hentTrygdetidGrunnlag(trygdetidId: UUID): List<TrygdetidGrunnlag> =
        using(sessionOf(dataSource)) { session ->
            queryOf(
                statement =
                    """
                    SELECT id, trygdetid_id, type, bosted, periode_fra, periode_til, kilde, beregnet_verdi,
                    beregnet_tidspunkt, beregnet_regelresultat , begrunnelse, poeng_inn_aar, poeng_ut_aar, prorata
                    FROM trygdetid_grunnlag
                    WHERE trygdetid_id = :trygdetidId
                    """.trimIndent(),
                paramMap = mapOf("trygdetidId" to trygdetidId),
            ).let { query ->
                session.run(
                    query.map { row -> row.toTrygdetidGrunnlag() }.asList,
                )
            }
        }

    private fun hentGrunnlagOpplysninger(trygdetidId: UUID): List<Opplysningsgrunnlag> =
        dataSource.transaction { tx ->
            tx.hentListe(
                queryString =
                    """
                    SELECT id, trygdetid_id, type, opplysning, kilde
                    FROM opplysningsgrunnlag
                    WHERE trygdetid_id = :trygdetidId
                    """.trimIndent(),
                params = { mapOf("trygdetidId" to trygdetidId) },
                converter = { it.toOpplysningsgrunnlag() },
            )
        }

    private fun hentTrygdetidMedIdNotNull(
        behandlingId: UUID,
        trygdetidId: UUID,
    ) = hentTrygdetidMedId(behandlingId, trygdetidId)
        ?: throw Exception("Fant ikke trygdetid for $behandlingId")

    private fun Row.toFaktiskTrygdetid(
        totalColumn: String,
        maanederColumn: String,
    ) = stringOrNull(totalColumn)?.let { period ->
        FaktiskTrygdetid(
            periode = Period.parse(period),
            antallMaaneder = long(maanederColumn),
        )
    }

    private fun Row.toFremtidigTrygdetid(
        totalColumn: String,
        maanederColumn: String,
        opptjeningsColumn: String,
        fireFemtedelerColumn: String,
    ) = stringOrNull(totalColumn)?.let { period ->
        FremtidigTrygdetid(
            periode = Period.parse(period),
            antallMaaneder = long(maanederColumn),
            opptjeningstidIMaaneder = long(opptjeningsColumn),
            mindreEnnFireFemtedelerAvOpptjeningstiden =
                boolean(fireFemtedelerColumn),
        )
    }

    private fun Row.toDetaljertBeregnetTrygdetid() =
        DetaljertBeregnetTrygdetid(
            resultat =
                DetaljertBeregnetTrygdetidResultat(
                    faktiskTrygdetidNorge =
                        this.toFaktiskTrygdetid(
                            totalColumn = "faktisk_trygdetid_norge_total",
                            maanederColumn = "faktisk_trygdetid_norge_antall_maaneder",
                        ),
                    faktiskTrygdetidTeoretisk =
                        this.toFaktiskTrygdetid(
                            totalColumn = "faktisk_trygdetid_teoretisk_total",
                            maanederColumn = "faktisk_trygdetid_teoretisk_antall_maaneder",
                        ),
                    fremtidigTrygdetidNorge =
                        this.toFremtidigTrygdetid(
                            totalColumn = "fremtidig_trygdetid_norge_total",
                            maanederColumn = "fremtidig_trygdetid_norge_antall_maaneder",
                            opptjeningsColumn = "fremtidig_trygdetid_norge_opptjeningstid_maaneder",
                            fireFemtedelerColumn = "fremtidig_trygdetid_norge_mindre_enn_fire_femtedeler",
                        ),
                    fremtidigTrygdetidTeoretisk =
                        this.toFremtidigTrygdetid(
                            totalColumn = "fremtidig_trygdetid_teoretisk_total",
                            maanederColumn = "fremtidig_trygdetid_teoretisk_antall_maaneder",
                            opptjeningsColumn = "fremtidig_trygdetid_teoretisk_opptjeningstid_maaneder",
                            fireFemtedelerColumn = "fremtidig_trygdetid_teoretisk_mindre_enn_fire_femtedeler",
                        ),
                    samletTrygdetidNorge = intOrNull("samlet_trygdetid_norge"),
                    samletTrygdetidTeoretisk = intOrNull("samlet_trygdetid_teoretisk"),
                    prorataBroek =
                        IntBroek.fra(
                            Pair(intOrNull("prorata_broek_teller"), intOrNull("prorata_broek_nevner")),
                        ),
                    overstyrt = boolean("beregnet_trygdetid_overstyrt"),
                    yrkesskade = boolean("yrkesskade"),
                    beregnetSamletTrygdetidNorge = intOrNull("beregnet_samlet_trygdetid_norge"),
                ),
            tidspunkt = tidspunkt("trygdetid_tidspunkt"),
            regelResultat =
                string("trygdetid_regelresultat").let { regelResultat ->
                    objectMapper.readTree(regelResultat)
                },
        )

    private fun Row.toTrygdetid(
        trygdetidGrunnlag: List<TrygdetidGrunnlag>,
        opplysninger: List<Opplysningsgrunnlag>,
    ) = Trygdetid(
        id = uuid("id"),
        sakId = long("sak_id"),
        behandlingId = uuid("behandling_id"),
        beregnetTrygdetid =
            stringOrNull("trygdetid_tidspunkt")?.let {
                this.toDetaljertBeregnetTrygdetid()
            },
        trygdetidGrunnlag = trygdetidGrunnlag,
        opplysninger = opplysninger,
        overstyrtNorskPoengaar = intOrNull("poengaar_overstyrt"),
        ident = string("ident"),
        yrkesskade = boolean("yrkesskade"),
    )

    private fun Row.toTrygdetidGrunnlag() =
        TrygdetidGrunnlag(
            id = uuid("id"),
            type = string("type").let { TrygdetidType.valueOf(it) },
            bosted = string("bosted"),
            periode =
                TrygdetidPeriode(
                    fra = localDate("periode_fra"),
                    til = localDate("periode_til"),
                ),
            kilde = string("kilde").let { objectMapper.readValue(it) },
            beregnetTrygdetid =
                stringOrNull("beregnet_verdi")?.let { verdi ->
                    BeregnetTrygdetidGrunnlag(
                        verdi = Period.parse(verdi),
                        tidspunkt = sqlTimestamp("beregnet_tidspunkt").toTidspunkt(),
                        regelResultat =
                            string("beregnet_regelresultat").let {
                                objectMapper.readTree(it)
                            },
                    )
                },
            begrunnelse = stringOrNull("begrunnelse"),
            poengInnAar = boolean("poeng_inn_aar"),
            poengUtAar = boolean("poeng_ut_aar"),
            prorata = boolean("prorata"),
        )

    private fun Row.toOpplysningsgrunnlag(): Opplysningsgrunnlag =
        Opplysningsgrunnlag(
            id = uuid("id"),
            type = string("type").let { TrygdetidOpplysningType.valueOf(it) },
            opplysning = string("opplysning").let { objectMapper.readValue(it) },
            kilde = string("kilde").let { objectMapper.readValue(it) },
        )
}
