import styled from 'styled-components'
import { Adresse } from './soeknadinfo/Adresse'
import { InfoWrapper } from './Soeknadsoversikt'
import { BarnetTilGode } from './gyldigSoeknad/BarnetTilGode'
import { Header } from '../styled'

export const SoeknadOversiktDel2: React.FC<any> = ({ gjenlevendeOgSoekerLikAdresse, gyldighet }) => {
  return (
    <>
      <Header>Kommer barnet til gode</Header>
      <Wrapper>
        <InfoWrapper>
          <Adresse gjenlevendeOgSoekerLikAdresse={gjenlevendeOgSoekerLikAdresse} />
        </InfoWrapper>
        <div className="soeknadGyldigFremsatt">
          <BarnetTilGode gyldighet={gyldighet} />
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
