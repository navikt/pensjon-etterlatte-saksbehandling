import { IDetaljertBehandling, IGyldighetResultat } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { GyldigFramsattBarnepensjon } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/barnepensjon/GyldigFramsattBarnepensjon'
import { GyldigFramsattOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/omstillingsstoenad/GyldigFramsattOmstillingsstoenad'
import { SakType } from '~shared/types/sak'

const gyldigFremsattTilStatusIcon = (gyldigFramsatt: IGyldighetResultat | undefined) => {
  if (gyldigFramsatt == undefined || gyldigFramsatt.resultat == undefined) {
    return 'warning'
  }
  switch (gyldigFramsatt.resultat) {
    case VurderingsResultat.OPPFYLT:
      return 'success'
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'error'
    default:
      return 'warning'
  }
}

export const OversiktGyldigFramsatt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  return behandling.sakType === SakType.BARNEPENSJON ? (
    <GyldigFramsattBarnepensjon
      gyldigFramsatt={behandling.gyldighetsprøving}
      gyldigFremsattTilStatusIcon={gyldigFremsattTilStatusIcon(behandling.gyldighetsprøving)}
    />
  ) : (
    <GyldigFramsattOmstillingsstoenad
      behandling={behandling}
      gyldigFremsattTilStatusIcon={gyldigFremsattTilStatusIcon(behandling.gyldighetsprøving)}
    />
  )
}
