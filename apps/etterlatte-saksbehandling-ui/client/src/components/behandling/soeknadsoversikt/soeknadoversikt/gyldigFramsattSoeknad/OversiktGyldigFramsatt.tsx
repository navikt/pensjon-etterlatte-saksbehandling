import { GyldigFramsattVurdering } from './GyldigFramsattVurdering'
import { Foreldreansvar } from './Foreldreansvar'
import { Innsender } from './Innsender'
import { Verge } from './Verge'
import { GyldigFramsattType, IGyldighetproving } from '../../../../../store/reducers/BehandlingReducer'
import { InfobokserWrapper, Header, VurderingsWrapper, SoeknadOversiktWrapper } from '../../styled'

export const OversiktGyldigFramsatt: React.FC<any> = ({ gyldigFramsatt }) => {
  const innsenderHarForeldreansvar = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.HAR_FORELDREANSVAR_FOR_BARNET
  )

  const innsenderErForelder = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER
  )

  return (
    <>
      <Header>Gyldig fremsatt</Header>
      <SoeknadOversiktWrapper>
        <InfobokserWrapper>
          <Innsender innsenderErForelder={innsenderErForelder} />
          <Foreldreansvar innsenderHarForeldreansvar={innsenderHarForeldreansvar} />
          <Verge />
        </InfobokserWrapper>
        <VurderingsWrapper>
          <GyldigFramsattVurdering
            gyldigFramsatt={gyldigFramsatt}
            innsenderErForelder={innsenderErForelder}
            innsenderHarForeldreansvar={innsenderHarForeldreansvar}
          />
        </VurderingsWrapper>
      </SoeknadOversiktWrapper>
    </>
  )
}
