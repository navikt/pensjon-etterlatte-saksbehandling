import styled from 'styled-components'
import { GyldighetType, VurderingsResultat, IGyldighetResultat } from '../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../shared/icons/gyldigIcon'
import { hentGyldighetsTekst } from './utils'

export const SoeknadGyldigFremsatt = ({ gyldighet }: { gyldighet: IGyldighetResultat }) => {
  const sjekkInfo = (): any => {
    const bostedadresse = gyldighet.vurderinger.find(
      (vilkaar) => vilkaar.navn === GyldighetType.BARN_GJENLEVENDE_SAMME_BOSTEDADRESSE_PDL
    )
    const foreldreransvar = gyldighet.vurderinger.find(
      (vilkaar) => vilkaar.navn === GyldighetType.HAR_FORELDREANSVAR_FOR_BARNET
    )
    const innsender = gyldighet.vurderinger.find((vilkaar) => vilkaar.navn === GyldighetType.INNSENDER_ER_FORELDER)

    return (
      gyldighet.resultat &&
      bostedadresse &&
      foreldreransvar &&
      innsender &&
      hentGyldighetsTekst(gyldighet.resultat, bostedadresse, foreldreransvar, innsender)
    )
  }
  return (
    <Wrapper>
      <div>{gyldighet.resultat && <GyldighetIcon status={gyldighet.resultat} large={true} />}</div>
      <div>
        <Title>Søknad gyldig fremsatt</Title>
        <Undertekst gray={true}>Automatisk {format(new Date(gyldighet.vurdertDato), 'dd.MM.yyyy')}</Undertekst>
        {gyldighet.resultat === VurderingsResultat.OPPFYLT ? (
          <Undertekst gray={false}>Ja søknad er gyldig fremsatt</Undertekst>
        ) : (
          <Undertekst gray={false}>{sjekkInfo()}</Undertekst>
        )}
      </div>
      {/* <Endre>
        <LockedIcon /> <span className="text">Endre</span>
      </Endre>
      */}
    </Wrapper>
  )
}

export const Wrapper = styled.div`
  display: flex;
  border-left: 4px solid #e5e5e5;
  margin-top: 50px;
  max-height: fit-content;
`
export const Endre = styled.div`
  display: inline-flex;
  height: 25px;
  display: inline-flex;
  cursor: pointer;
  color: #0056b4;

  .text {
    padding-bottom: 10px;
  }
`

export const Title = styled.div`
  display: flex;
  font-size: 18px;
  font-weight: bold;
`
export const Undertekst = styled.div<{ gray: boolean }>`
  display: flex;
  margin-bottom: 1em;
  margin-top: 0.3em;
  max-width: 18em;
  color: ${(props) => (props.gray ? '#707070' : '#000000')};
`
