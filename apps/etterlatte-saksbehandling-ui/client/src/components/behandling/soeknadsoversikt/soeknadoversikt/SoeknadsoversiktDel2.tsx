import styled from 'styled-components'
import { Adresse } from './soeknadinfo/Adresse'
import { InfoWrapper } from './Soeknadsoversikt'
import { BarnetTilGode } from './gyldigSoeknad/BarnetTilGode'
import { IKommerSoekerTilgode, VilkaarsType } from '../../../../store/reducers/BehandlingReducer'

export const SoeknadOversiktKommerSoekerTilgode = ({
  kommerSoekerTilgode,
}: {
  kommerSoekerTilgode: IKommerSoekerTilgode
}) => {
  const sammeAdresse = kommerSoekerTilgode.kommerSoekerTilgodeVurdering.vilkaar.find(
    (vilkaar) => vilkaar.navn === VilkaarsType.SAMME_ADRESSE
  )
  return (
    <>
      <Header>Kommer barnet til gode</Header>
      <Wrapper>
        <InfoWrapper>
          <Adresse gjenlevendeOgSoekerLikAdresse={sammeAdresse} />
        </InfoWrapper>
        <div className="soeknadGyldigFremsatt">
          <BarnetTilGode
            sammeAdresse={sammeAdresse}
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
