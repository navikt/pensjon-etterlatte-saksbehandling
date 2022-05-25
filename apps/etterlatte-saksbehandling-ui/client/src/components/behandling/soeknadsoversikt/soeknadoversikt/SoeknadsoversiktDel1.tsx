import styled from 'styled-components'
import { SoeknadGyldigFremsatt } from './gyldigSoeknad/SoeknadGyldigFremsatt'
import { Foreldreansvar } from './soeknadinfo/Foreldreansvar'
import { Innsender } from './soeknadinfo/Innsender'
import { Verge } from './soeknadinfo/Verge'
import { InfoWrapper } from './Soeknadsoversikt'

export const SoeknadOversiktDel1: React.FC<any> = ({ innsenderHarForeldreansvar, innsenderErForelder, gyldighet }) => {
  return (
    <>
      <Header>Gyldig fremsatt</Header>
      <Wrapper>
        <InfoWrapper>
          <Innsender innsenderErForelder={innsenderErForelder} />
          <Foreldreansvar innsenderHarForeldreansvar={innsenderHarForeldreansvar} />
          <Verge />
        </InfoWrapper>
        <div className="soeknadGyldigFremsatt">
          <SoeknadGyldigFremsatt
            gyldighet={gyldighet}
            innsenderErForelder={innsenderErForelder}
            innsenderHarForeldreansvar={innsenderHarForeldreansvar}
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
  margin-top: 2em;
`
