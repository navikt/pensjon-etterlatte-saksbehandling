package no.nav.etterlatte.vilkaarsvurdering

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import java.util.*

class DelvilkaarRepository {

    internal fun oppdaterDelvilkaar(vurdertVilkaar: VurdertVilkaar, tx: TransactionalSession) {
        lagreDelvilkaarResultat(vurdertVilkaar.vilkaarId, vurdertVilkaar.hovedvilkaar, tx)
        tilbakestillAlleUnntaksvilkaarTilNull(vurdertVilkaar.vilkaarId, tx)
        vurdertVilkaar.unntaksvilkaar?.let { vilkaar ->
            lagreDelvilkaarResultat(vurdertVilkaar.vilkaarId, vilkaar, tx)
        }
        if (vurdertVilkaar.hovedvilkaarOgUnntaksvilkaarIkkeOppfylt()) {
            settAlleUnntaksvilkaarSomIkkeOppfyltHvisVilkaaretIkkeErOppfyltOgIngenUnntakTreffer(vurdertVilkaar, tx)
        }
    }

    private fun lagreDelvilkaarResultat(
        vilkaarId: UUID,
        vilkaarTypeOgUtfall: VilkaarTypeOgUtfall,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = """
            UPDATE delvilkaar
            SET resultat = :resultat
            WHERE vilkaar_id = :vilkaar_id AND vilkaar_type = :vilkaar_type
        """,
            paramMap = mapOf(
                "vilkaar_id" to vilkaarId,
                "vilkaar_type" to vilkaarTypeOgUtfall.type.name,
                "resultat" to vilkaarTypeOgUtfall.resultat.name
            )
        ).let { tx.run(it.asUpdate) }
    }

    private fun tilbakestillAlleUnntaksvilkaarTilNull(
        vilkaarId: UUID,
        tx: TransactionalSession
    ) =
        queryOf(
            statement = """
            UPDATE delvilkaar
            SET resultat = null
            WHERE vilkaar_id = :vilkaar_id AND hovedvilkaar != true and resultat is not null
        """,
            paramMap = mapOf(
                "vilkaar_id" to vilkaarId,
                "oppfylt_resultat" to Utfall.OPPFYLT.name
            )
        ).let { tx.run(it.asUpdate) }

    private fun settAlleUnntaksvilkaarSomIkkeOppfyltHvisVilkaaretIkkeErOppfyltOgIngenUnntakTreffer(
        vurdertVilkaar: VurdertVilkaar,
        tx: TransactionalSession
    ) {
        queryOf(
            statement = """
            UPDATE delvilkaar
            SET resultat = :resultat
            WHERE vilkaar_id = :vilkaar_id AND hovedvilkaar != true
        """,
            paramMap = mapOf(
                "vilkaar_id" to vurdertVilkaar.vilkaarId,
                "resultat" to Utfall.IKKE_OPPFYLT.name
            )
        ).let { tx.run(it.asUpdate) }
    }

    internal fun opprettVilkaarsvurdering(vilkaarId: UUID, vilkaar: Vilkaar, tx: TransactionalSession) {
        lagreDelvilkaar(vilkaarId, vilkaar.hovedvilkaar, true, tx)
        vilkaar.unntaksvilkaar.forEach { unntaksvilkaar ->
            lagreDelvilkaar(vilkaarId, unntaksvilkaar, false, tx)
        }
    }

    private fun lagreDelvilkaar(
        vilkaarId: UUID,
        delvilkaar: Delvilkaar,
        hovedvilkaar: Boolean,
        tx: TransactionalSession
    ) = queryOf(
        statement = """
            INSERT INTO delvilkaar(vilkaar_id, vilkaar_type, hovedvilkaar, tittel, beskrivelse, spoersmaal, paragraf, 
                ledd, bokstav, lenke, resultat) 
            VALUES(:vilkaar_id, :vilkaar_type, :hovedvilkaar, :tittel, :beskrivelse, :spoersmaal, :paragraf, :ledd, 
                :bokstav, :lenke, :resultat)
        """,
        paramMap = mapOf(
            "vilkaar_id" to vilkaarId,
            "vilkaar_type" to delvilkaar.type.name,
            "hovedvilkaar" to hovedvilkaar,
            "tittel" to delvilkaar.tittel,
            "beskrivelse" to delvilkaar.beskrivelse,
            "spoersmaal" to delvilkaar.spoersmaal,
            "paragraf" to delvilkaar.lovreferanse.paragraf,
            "ledd" to delvilkaar.lovreferanse.ledd,
            "bokstav" to delvilkaar.lovreferanse.paragraf,
            "lenke" to delvilkaar.lovreferanse.lenke,
            "resultat" to delvilkaar.resultat?.name
        )
    ).let { tx.run(it.asExecute) }

    internal fun hentDelvilkaar(vilkaarId: UUID, hovedvilkaar: Boolean, session: Session): List<Delvilkaar> =
        queryOf(
            """
            SELECT vilkaar_id, vilkaar_type, hovedvilkaar, tittel, beskrivelse, spoersmaal, paragraf, ledd, bokstav, 
                lenke, resultat 
            FROM delvilkaar WHERE vilkaar_id = :vilkaar_id AND hovedvilkaar = :hovedvilkaar
        """,
            mapOf("vilkaar_id" to vilkaarId, "hovedvilkaar" to hovedvilkaar)
        )
            .let { query -> session.run(query.map { row -> row.toDelvilkaar() }.asList) }
            .sortedBy { it.type.rekkefoelge }

    internal fun slettDelvilkaarResultat(vilkaarId: UUID, tx: TransactionalSession) {
        queryOf(
            """
            UPDATE delvilkaar
            SET resultat = null
            WHERE vilkaar_id = :vilkaar_id
        """,
            mapOf("vilkaar_id" to vilkaarId)
        )
            .let { tx.run(it.asUpdate) }
    }

    private fun Row.toDelvilkaar() =
        Delvilkaar(
            type = VilkaarType.valueOf(string("vilkaar_type")),
            tittel = string("tittel"),
            beskrivelse = stringOrNull("beskrivelse"),
            spoersmaal = stringOrNull("spoersmaal"),
            lovreferanse = Lovreferanse(
                paragraf = string("paragraf"),
                ledd = intOrNull("ledd"),
                bokstav = stringOrNull("bokstav"),
                lenke = stringOrNull("lenke")
            ),
            resultat = stringOrNull("resultat")?.let { Utfall.valueOf(it) }
        )
}