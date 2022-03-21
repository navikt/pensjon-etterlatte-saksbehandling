import { Content, ContentHeader } from '../../../shared/styled'
import { OmSoeknad } from './oversikt/OmSoeknad'
import { Familieforhold } from './oversikt/Familieforhold'
import { SoeknadGyldigFremsatt } from './oversikt/SoeknadGyldigFremsatt'
import { BehandlingHandlingKnapper } from '../BehandlingHandlingKnapper'

export const Soeknadsoversikt = () => {
  return (
    <Content>
      <ContentHeader>
        <OmSoeknad />
        <Familieforhold />
        <SoeknadGyldigFremsatt />
      </ContentHeader>
      <BehandlingHandlingKnapper />
    </Content>
  )
}
