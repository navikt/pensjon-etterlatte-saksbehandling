import { Button } from '@navikt/ds-react'
import { Content, ContentHeader } from '../../../shared/styled'
import { useBehandlingRoutes } from '../BehandlingRoutes'

export const Utbetalingsoversikt = () => {
  const { next } = useBehandlingRoutes()
  return (
    <Content>
      <ContentHeader>
        <h1>Utbetalingsoversikt</h1>
      </ContentHeader>
    </Content>
  )
}
