import { IGyldighetResultat } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { GyldigFramsattBarnepensjon } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/barnepensjon/GyldigFramsattBarnepensjon'
import { GyldigFramsattOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/omstillingsstoenad/GyldigFramsattOmstillingsstoenad'
import { ISaksType } from '~components/behandling/fargetags/saksType'

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

export const OversiktGyldigFramsatt = ({
  gyldigFramsatt,
  sakType,
}: {
  gyldigFramsatt: IGyldighetResultat | undefined
  sakType: ISaksType
}) => {
  return sakType === ISaksType.BARNEPENSJON ? (
    <GyldigFramsattBarnepensjon
      gyldigFramsatt={gyldigFramsatt}
      gyldigFremsattTilStatusIcon={gyldigFremsattTilStatusIcon(gyldigFramsatt)}
    />
  ) : (
    <GyldigFramsattOmstillingsstoenad
      gyldigFramsatt={gyldigFramsatt}
      gyldigFremsattTilStatusIcon={gyldigFremsattTilStatusIcon(gyldigFramsatt)}
    />
  )
}
