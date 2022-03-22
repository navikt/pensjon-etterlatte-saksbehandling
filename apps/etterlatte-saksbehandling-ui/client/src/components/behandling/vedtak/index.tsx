import { Button } from '@navikt/ds-react'
import { Content, ContentHeader } from '../../../shared/styled'
import { useBehandlingRoutes } from '../BehandlingRoutes'

export const Vedtak = () => {
  const { next } = useBehandlingRoutes()
  return (
    <Content>
      <ContentHeader>
        <h1>Vedtak</h1>
      </ContentHeader>
    </Content>
  )
}
