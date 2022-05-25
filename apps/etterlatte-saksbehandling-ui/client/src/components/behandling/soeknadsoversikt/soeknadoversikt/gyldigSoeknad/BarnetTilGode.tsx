import styled from 'styled-components'
import {
  GyldigFramsattType,
  VurderingsResultat,
  IGyldighetResultat,
} from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { hentGyldigBostedTekst } from '../../utils'
import { Title, Undertekst } from './styled'

export const BarnetTilGode = ({ gyldighet }: { gyldighet: IGyldighetResultat }) => {
  const bostedadresse = gyldighet.vurderinger.find(
    (vilkaar) => vilkaar.navn === GyldigFramsattType.BARN_GJENLEVENDE_SAMME_BOSTEDADRESSE_PDL
  )

  const sjekkInfo = (): any => {
    return gyldighet.resultat && bostedadresse && hentGyldigBostedTekst(bostedadresse)
  }
  return (
    <Wrapper>
      {bostedadresse && (
        <div>{gyldighet.resultat && <GyldighetIcon status={bostedadresse.resultat} large={true} />}</div>
      )}
      <div>
        <Title>Pensjon kommer barnet til gode</Title>
        <Undertekst gray={true}>Automatisk {format(new Date(gyldighet.vurdertDato), 'dd.MM.yyyy')}</Undertekst>
        {bostedadresse && bostedadresse.resultat !== VurderingsResultat.OPPFYLT && (
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
  height: fit-content;
  width: 350px;
`
