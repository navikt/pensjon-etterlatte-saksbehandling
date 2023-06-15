package no.nav.etterlatte.institusjonsopphold

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

data class InstitusjonsoppholdService(private val institusjonsoppholdDao: InstitusjonsoppholdDao) {

    fun leggInnInstitusjonsoppholdBegrunnelse(
        sakId: Long,
        saksbehandler: Grunnlagsopplysning.Saksbehandler,
        institusjonoppholdBegrunnelse: InstitusjonsoppholdBegrunnelse
    ) {
        inTransaction {
            institusjonsoppholdDao.lagreInstitusjonsopphold(sakId, saksbehandler, institusjonoppholdBegrunnelse)
        }
    }

    fun hentInstitusjonsoppholdBegrunnelse(
        grunnlagsEndringshendelseId: String
    ): InstitusjonsoppholdBegrunnelseMedSaksbehandler? {
        return inTransaction {
            institusjonsoppholdDao.hentBegrunnelse(grunnlagsEndringshendelseId)
        }
    }
}

data class InstitusjonsoppholdBegrunnelse(
    val kanGiReduksjonAvYtelse: JaNeiMedBegrunnelse,
    val forventetVarighetMerEnn3Maaneder: JaNeiMedBegrunnelse,
    val grunnlagsEndringshendelseId: String
)

data class InstitusjonsoppholdBegrunnelseMedSaksbehandler(
    val kanGiReduksjonAvYtelse: JaNeiMedBegrunnelse,
    val forventetVarighetMerEnn3Maaneder: JaNeiMedBegrunnelse,
    val grunnlagsEndringshendelseId: String,
    val saksbehandler: Grunnlagsopplysning.Saksbehandler
)