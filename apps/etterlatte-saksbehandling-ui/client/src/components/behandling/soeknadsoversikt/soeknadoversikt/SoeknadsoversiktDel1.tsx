import styled from 'styled-components'
import { SoeknadGyldigFremsatt } from './gyldigSoeknad/SoeknadGyldigFremsatt'
import { Foreldreansvar } from './soeknadinfo/Foreldreansvar'
import { Innsender } from './soeknadinfo/Innsender'
import { Verge } from './soeknadinfo/Verge'
import { InfoWrapper } from './Soeknadsoversikt'

export const SoeknadOversiktDel1: React.FC<any> = ({
  innsender,
  gjenlevendePdl,
  gjenlevendeHarForeldreansvar,
  innsenderHarForeldreAnsvar,
  gyldighet,
}) => {
  return (
    <>
      <Header>Gyldig fremsatt</Header>
      <Wrapper>
        <InfoWrapper>
          <Innsender innsender={innsender} innsenderHarForeldreAnsvar={innsenderHarForeldreAnsvar} />
          <Foreldreansvar gjenlevendePdl={gjenlevendePdl} gjenlevendeHarForeldreansvar={gjenlevendeHarForeldreansvar} />
          <Verge />
        </InfoWrapper>
        <div className="soeknadGyldigFremsatt">
          <SoeknadGyldigFremsatt gyldighet={gyldighet} />
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
  margin-top: 2em;
`
