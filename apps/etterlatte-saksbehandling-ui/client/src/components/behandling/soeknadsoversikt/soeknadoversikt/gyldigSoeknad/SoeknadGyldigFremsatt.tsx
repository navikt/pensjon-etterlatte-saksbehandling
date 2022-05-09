import { GyldighetType, VurderingsResultat, IGyldighetResultat } from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { mapGyldighetstyperTilTekst } from '../../utils'
import { Title, Undertekst, Wrapper } from './styled'

export const SoeknadGyldigFremsatt = ({ gyldighet }: { gyldighet: IGyldighetResultat }) => {
  const sjekkInfo = (): any => {
    const foreldreransvar = gyldighet.vurderinger.find(
      (vilkaar) => vilkaar.navn === GyldighetType.HAR_FORELDREANSVAR_FOR_BARNET
    )
    const innsender = gyldighet.vurderinger.find((vilkaar) => vilkaar.navn === GyldighetType.INNSENDER_ER_FORELDER)

    const harForeldreansvar = foreldreransvar && mapGyldighetstyperTilTekst(foreldreransvar)
    const erForelder = innsender && mapGyldighetstyperTilTekst(innsender)
    return harForeldreansvar ? harForeldreansvar : erForelder
  }
  return (
    <Wrapper>
      <div>{gyldighet.resultat && <GyldighetIcon status={gyldighet.resultat} large={true} />}</div>
      <div>
        <Title>Søknad gyldig fremsatt</Title>
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
