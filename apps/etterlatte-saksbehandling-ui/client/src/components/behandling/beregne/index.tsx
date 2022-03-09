import { Button } from '@navikt/ds-react'
import { Content, ContentHeader } from '../../../shared/styled'
import { useBehandlingRoutes } from '../BehandlingRoutes'

export const Beregne = () => {
  const { next } = useBehandlingRoutes()
  return (
    <Content>
      <ContentHeader>
        <h1>Beregne</h1>
        <Button variant="primary" size="medium" className="button" onClick={next}>
          Bekreft og gå videre
        </Button>
      </ContentHeader>
    </Content>
  )
}
