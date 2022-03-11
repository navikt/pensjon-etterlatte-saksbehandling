import { Content, ContentHeader } from '../../../shared/styled'
import { OmSoeknad } from './OmSoeknad'
import { Familieforhold } from './Familieforhold'
import { SoeknadGyldigFremsatt } from './SoeknadGyldigFremsatt'

export const Soeknadsoversikt = () => {
  return (
    <Content>
      <ContentHeader>
        <OmSoeknad />
        <Familieforhold />
        <SoeknadGyldigFremsatt />
      </ContentHeader>
    </Content>
  )
}
