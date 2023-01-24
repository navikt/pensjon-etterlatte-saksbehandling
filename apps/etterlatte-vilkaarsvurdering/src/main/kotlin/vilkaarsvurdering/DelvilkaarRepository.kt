package no.nav.etterlatte.vilkaarsvurdering

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Delvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Lovreferanse
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Unntaksvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Vilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import java.util.*

internal class DelvilkaarRepository {

    internal fun oppdaterDelvilkaar(vurdertVilkaar: VurdertVilkaar, tx: TransactionalSession) {
        lagreDelvilkaarResultat(vurdertVilkaar.vilkaarId, vurdertVilkaar.hovedvilkaar, tx)
        settAndreOppfylteDelvilkaarSomIkkeOppfylt(vurdertVilkaar.vilkaarId, tx)
        vurdertVilkaar.unntaksvilkaar?.let { vilkaar ->
            lagreDelvilkaarResultat(vurdertVilkaar.vilkaarId, vilkaar, tx)
        }
        // Alle unntaksvilkår settes til IKKE_OPPFYLT hvis ikke hovedvilkår eller unntaksvilkår er oppfylt
        // Dvs. vilkåret er ikke oppfylt og ingen av unntaka treffer
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

    private fun settAndreOppfylteDelvilkaarSomIkkeOppfylt(
        vilkaarId: UUID,
        tx: TransactionalSession
    ) =
        queryOf(
            statement = """
            UPDATE delvilkaar
            SET resultat = :resultat
            WHERE vilkaar_id = :vilkaar_id AND hovedvilkaar != true and resultat = :oppfylt_resultat
        """,
            paramMap = mapOf(
                "vilkaar_id" to vilkaarId,
                "resultat" to Utfall.IKKE_OPPFYLT.name,
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
        vilkaar.unntaksvilkaar?.forEach { unntaksvilkaar ->
            lagreDelvilkaar(vilkaarId, unntaksvilkaar, false, tx)
        }
    }

    private fun lagreDelvilkaar(
        vilkaarId: UUID,
        unntaksvilkaar: Unntaksvilkaar,
        hovedvilkaar: Boolean,
        tx: TransactionalSession
    ) = queryOf(
        statement = """
            INSERT INTO delvilkaar(vilkaar_id, vilkaar_type, hovedvilkaar, tittel, beskrivelse, paragraf, ledd, bokstav, lenke, resultat) 
            VALUES(:vilkaar_id, :vilkaar_type, :hovedvilkaar, :tittel, :beskrivelse, :paragraf, :ledd, :bokstav, :lenke, :resultat)
        """,
        paramMap = mapOf(
            "vilkaar_id" to vilkaarId,
            "vilkaar_type" to unntaksvilkaar.type.name,
            "hovedvilkaar" to hovedvilkaar,
            "tittel" to unntaksvilkaar.tittel,
            "beskrivelse" to unntaksvilkaar.beskrivelse,
            "paragraf" to unntaksvilkaar.lovreferanse.paragraf,
            "ledd" to unntaksvilkaar.lovreferanse.ledd,
            "bokstav" to unntaksvilkaar.lovreferanse.paragraf,
            "lenke" to unntaksvilkaar.lovreferanse.lenke,
            "resultat" to unntaksvilkaar.resultat?.name
        )
    ).let { tx.run(it.asExecute) }

    internal fun hentDelvilkaar(vilkaarId: UUID, hovedvilkaar: Boolean, session: Session): List<Delvilkaar> =
        queryOf(
            """
            SELECT vilkaar_id, vilkaar_type, hovedvilkaar, tittel, beskrivelse, paragraf, ledd, bokstav, lenke, resultat 
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
            lovreferanse = Lovreferanse(
                paragraf = string("paragraf"),
                ledd = intOrNull("ledd"),
                bokstav = stringOrNull("bokstav"),
                lenke = stringOrNull("lenke")
            ),
            resultat = stringOrNull("resultat")?.let { Utfall.valueOf(it) }
        )
}