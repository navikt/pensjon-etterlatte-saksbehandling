import {
  VurderingsResultat,
  IGyldighetResultat,
  IGyldighetproving,
} from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { mapGyldighetstyperTilTekst } from '../../utils'
import { Title, Undertekst, Wrapper } from './styled'

export const GyldigFramsattVurdering = ({
  gyldigFramsatt,
  innsenderHarForeldreansvar,
  innsenderErForelder,
}: {
  gyldigFramsatt: IGyldighetResultat
  innsenderHarForeldreansvar: IGyldighetproving | undefined
  innsenderErForelder: IGyldighetproving | undefined
}) => {
  const sjekkInfo = (): any => {
    const harForeldreansvar = innsenderHarForeldreansvar && mapGyldighetstyperTilTekst(innsenderHarForeldreansvar)
    const erForelder = innsenderErForelder && mapGyldighetstyperTilTekst(innsenderErForelder)
    return harForeldreansvar ? harForeldreansvar : erForelder
  }
  const tittel =
    gyldigFramsatt.resultat !== VurderingsResultat.OPPFYLT ? 'Søknad ikke gyldig fremsatt' : 'Søknad gyldig fremsatt'

  return (
    <Wrapper>
      <div>{gyldigFramsatt.resultat && <GyldighetIcon status={gyldigFramsatt.resultat} large={true} />}</div>
      <div>
        <Title>{tittel}</Title>
        <Undertekst gray={true}>Automatisk {format(new Date(gyldigFramsatt.vurdertDato), 'dd.MM.yyyy')}</Undertekst>
        {gyldigFramsatt.resultat !== VurderingsResultat.OPPFYLT && <Undertekst gray={false}>{sjekkInfo()}</Undertekst>}
      </div>
    </Wrapper>
  )
}
