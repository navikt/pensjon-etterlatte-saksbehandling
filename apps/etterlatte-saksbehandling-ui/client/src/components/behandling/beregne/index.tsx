import { Button } from '@navikt/ds-react'
import { Content, ContentHeader } from '../../../shared/styled'
import { useBehandlingRoutes } from '../BehandlingRoutes'

export const Beregne = () => {
  const { next } = useBehandlingRoutes()
  return (
    <Content>
      <ContentHeader>
        <h1>Beregne</h1>
      </ContentHeader>
    </Content>
  )
}
