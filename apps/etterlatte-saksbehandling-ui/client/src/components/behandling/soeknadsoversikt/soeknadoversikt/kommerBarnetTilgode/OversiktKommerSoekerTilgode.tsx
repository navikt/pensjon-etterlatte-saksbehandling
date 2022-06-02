import styled from 'styled-components'
import { Adresse } from './Adresse'
import { InfoWrapper } from '../Soeknadsoversikt'
import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { IKommerSoekerTilgode, VilkaarsType } from '../../../../../store/reducers/BehandlingReducer'

export const OversiktKommerSoekerTilgode = ({ kommerSoekerTilgode }: { kommerSoekerTilgode: IKommerSoekerTilgode }) => {
  const gjenlevendeOgSoekerLikAdresse = kommerSoekerTilgode.kommerSoekerTilgodeVurdering.vilkaar.find(
    (vilkaar) => vilkaar.navn === VilkaarsType.SAMME_ADRESSE
  )
  return (
    <>
      <Header>Vurdering om pensjonen kommer barnet til gode</Header>
      <Wrapper>
        <InfoWrapper>
          <Adresse gjenlevendeOgSoekerLikAdresse={gjenlevendeOgSoekerLikAdresse} />
        </InfoWrapper>
        <div className="soeknadGyldigFremsatt">
          <KommerBarnetTilGodeVurdering
            gjenlevendeOgSoekerLikAdresse={gjenlevendeOgSoekerLikAdresse}
            kommerSoekerTilgodeVurdering={kommerSoekerTilgode.kommerSoekerTilgodeVurdering}
          />
        </div>
      </Wrapper>
    </>
  )
}

export const Wrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;

  .soeknadGyldigFremsatt {
    margin-right: 2em;
  }
`

export const Header = styled.div`
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 1em;
  margin-top: 0em;
`
