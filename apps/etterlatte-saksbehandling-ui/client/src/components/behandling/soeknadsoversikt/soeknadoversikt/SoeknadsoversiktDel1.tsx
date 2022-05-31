import styled from 'styled-components'
import { SoeknadGyldigFremsatt } from './gyldigSoeknad/SoeknadGyldigFremsatt'
import { Foreldreansvar } from './soeknadinfo/Foreldreansvar'
import { Innsender } from './soeknadinfo/Innsender'
import { Verge } from './soeknadinfo/Verge'
import { InfoWrapper } from './Soeknadsoversikt'
import { GyldigFramsattType, IGyldighetproving } from '../../../../store/reducers/BehandlingReducer'

export const SoeknadOversiktGyldigFramsatt: React.FC<any> = ({ gyldigFramsatt }) => {
  const innsenderHarForeldreansvar = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.HAR_FORELDREANSVAR_FOR_BARNET
  )

  const innsenderErForelder = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER
  )

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
            gyldigFramsatt={gyldigFramsatt}
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
