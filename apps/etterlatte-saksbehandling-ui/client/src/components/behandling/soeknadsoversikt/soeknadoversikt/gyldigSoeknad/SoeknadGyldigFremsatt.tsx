import {
  VurderingsResultat,
  IGyldighetResultat,
  IGyldighetproving,
} from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { mapGyldighetstyperTilTekst } from '../../utils'
import { Title, Undertekst, Wrapper } from './styled'

export const SoeknadGyldigFremsatt = ({
  gyldighet,
  innsenderHarForeldreansvar,
  innsenderErForelder,
}: {
  gyldighet: IGyldighetResultat
  innsenderHarForeldreansvar: IGyldighetproving | undefined
  innsenderErForelder: IGyldighetproving | undefined
}) => {
  const sjekkInfo = (): any => {
    const harForeldreansvar = innsenderHarForeldreansvar && mapGyldighetstyperTilTekst(innsenderHarForeldreansvar)
    const erForelder = innsenderErForelder && mapGyldighetstyperTilTekst(innsenderErForelder)
    return harForeldreansvar ? harForeldreansvar : erForelder
  }
  const tittel =
    gyldighet.resultat !== VurderingsResultat.OPPFYLT ? 'Søknad ikke gyldig fremsatt' : 'Søknad gyldig fremsatt'

  return (
    <Wrapper>
      <div>{gyldighet.resultat && <GyldighetIcon status={gyldighet.resultat} large={true} />}</div>
      <div>
        <Title>{tittel}</Title>
        <Undertekst gray={true}>Automatisk {format(new Date(gyldighet.vurdertDato), 'dd.MM.yyyy')}</Undertekst>
        {gyldighet.resultat !== VurderingsResultat.OPPFYLT && <Undertekst gray={false}>{sjekkInfo()}</Undertekst>}
      </div>
      {/* <Endre>
        <LockedIcon /> <span className="text">Endre</span>
      </Endre>
      */}
    </Wrapper>
  )
}
