package no.nav.etterlatte.institusjonsopphold

import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.JaNei
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
}

data class InstitusjonsoppholdBegrunnelse(
    val kanGiReduksjonAvYtelse: JaNei,
    val kanGiReduksjonAvYtelseBegrunnelse: String,
    val forventetVarighetMerEnn3Maaneder: JaNei,
    val forventetVarighetMerEnn3MaanederBegrunnelse: String
)