import { IDetaljertBehandling, IGyldighetResultat } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { GyldigFramsattBarnepensjon } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/barnepensjon/GyldigFramsattBarnepensjon'
import { GyldigFramsattOmstillingsstoenad } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/omstillingsstoenad/GyldigFramsattOmstillingsstoenad'
import { SakType } from '~shared/types/sak'
import { StatusIconProps } from '~shared/icons/statusIcon'
import { Personopplysninger } from '~shared/types/grunnlag'

const gyldigFremsattTilStatusIcon = (gyldigFramsatt: IGyldighetResultat | undefined): StatusIconProps => {
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
  behandling,
  personopplysninger,
}: {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger
}) => {
  return behandling.sakType === SakType.BARNEPENSJON ? (
    <GyldigFramsattBarnepensjon
      behandling={behandling}
      familieforhold={{
        avdoede: personopplysninger.avdoede,
        gjenlevende: personopplysninger.gjenlevende,
        soeker: personopplysninger.soeker,
      }}
      innsender={personopplysninger.innsender}
      gyldigFramsatt={behandling.gyldighetsprøving}
      gyldigFremsattTilStatusIcon={gyldigFremsattTilStatusIcon(behandling.gyldighetsprøving)}
    />
  ) : (
    <GyldigFramsattOmstillingsstoenad
      behandling={behandling}
      innsender={personopplysninger.innsender}
      gyldigFremsattTilStatusIcon={gyldigFremsattTilStatusIcon(behandling.gyldighetsprøving)}
    />
  )
}
