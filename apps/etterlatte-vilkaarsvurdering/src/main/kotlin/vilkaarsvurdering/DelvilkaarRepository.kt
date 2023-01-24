package no.nav.etterlatte.vilkaarsvurdering

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Unntaksvilkaar
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import java.util.*

internal class DelvilkaarRepository {

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
        // Alle unntaksvilkår settes til IKKE_OPPFYLT hvis ikke hovedvilkår eller unntaksvilkår er oppfylt
        if (vurdertVilkaar.hovedvilkaarOgUnntaksvilkaarIkkeOppfylt()) {
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
    }

    internal fun oppdaterDelvilkaar(vurdertVilkaar: VurdertVilkaar, tx: TransactionalSession) {
        lagreDelvilkaarResultat(vurdertVilkaar.vilkaarId, vurdertVilkaar.hovedvilkaar, tx)
        settAndreOppfylteDelvilkaarSomIkkeOppfylt(vurdertVilkaar.vilkaarId, tx)
        vurdertVilkaar.unntaksvilkaar?.let { vilkaar ->
            lagreDelvilkaarResultat(vurdertVilkaar.vilkaarId, vilkaar, tx)
        } ?: run {
            settAlleUnntaksvilkaarSomIkkeOppfyltHvisVilkaaretIkkeErOppfyltOgIngenUnntakTreffer(
                vurdertVilkaar,
                tx
            )
        }
    }

    internal fun lagreDelvilkaar(
        vilkaarId: UUID,
        unntaksvilkaar: Unntaksvilkaar,
        hovedvilkaar: Boolean,
        tx: TransactionalSession
    ) = queryOf(
        statement = lagreDelvilkaar,
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

    val lagreDelvilkaar = """
            INSERT INTO delvilkaar(vilkaar_id, vilkaar_type, hovedvilkaar, tittel, beskrivelse, paragraf, ledd, bokstav, lenke, resultat) 
            VALUES(:vilkaar_id, :vilkaar_type, :hovedvilkaar, :tittel, :beskrivelse, :paragraf, :ledd, :bokstav, :lenke, :resultat)
        """

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
}