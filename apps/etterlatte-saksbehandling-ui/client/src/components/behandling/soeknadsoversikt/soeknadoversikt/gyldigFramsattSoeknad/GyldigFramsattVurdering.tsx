import {
  IGyldighetproving,
  IGyldighetResultat,
  VurderingsResultat,
} from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { hentGyldighetsTekst } from '../../utils'
import { VurderingsContainer, VurderingsTitle, Undertekst } from '../../styled'

export const GyldigFramsattVurdering = ({
  gyldigFramsatt,
  innsenderHarForeldreansvar,
  innsenderErForelder,
  ingenAnnenVergeEnnForelder,
}: {
  gyldigFramsatt: IGyldighetResultat
  innsenderHarForeldreansvar: IGyldighetproving | undefined
  innsenderErForelder: IGyldighetproving | undefined
  ingenAnnenVergeEnnForelder: IGyldighetproving | undefined
}) => {
  const hentTekst = (): string | undefined => {
    if (gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT) {
      return undefined
    } else {
      return hentGyldighetsTekst(
        innsenderErForelder?.resultat,
        innsenderHarForeldreansvar?.resultat,
        ingenAnnenVergeEnnForelder?.resultat
      )
    }
  }

  const tittel =
    gyldigFramsatt.resultat !== VurderingsResultat.OPPFYLT ? 'Søknad ikke gyldig fremsatt' : 'Søknad gyldig fremsatt'
  const vurderingstekst = hentTekst()

  return (
    <VurderingsContainer>
      <div>{gyldigFramsatt.resultat && <GyldighetIcon status={gyldigFramsatt.resultat} large={true} />}</div>
      <div>
        <VurderingsTitle>{tittel}</VurderingsTitle>
        <Undertekst gray={true}>Automatisk {format(new Date(gyldigFramsatt.vurdertDato), 'dd.MM.yyyy')}</Undertekst>
        {gyldigFramsatt.resultat !== VurderingsResultat.OPPFYLT && (
          <Undertekst gray={false}>{vurderingstekst}</Undertekst>
        )}
      </div>
    </VurderingsContainer>
  )
}
