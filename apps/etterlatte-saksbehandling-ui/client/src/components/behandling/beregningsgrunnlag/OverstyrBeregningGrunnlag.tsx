import { Content, ContentHeader } from '~shared/styled'
import { OverstyrBeregning } from '~shared/types/Beregning'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { HeadingWrapper } from '../soeknadsoversikt/styled'
import { BodyShort, Heading } from '@navikt/ds-react'
import styled from 'styled-components'

const OverstyrBeregningGrunnlag = (props: {
  behandling: IDetaljertBehandling
  overstyrBeregning: OverstyrBeregning
}) => {
  const { overstyrBeregning } = props

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading size="medium" level="2">
            Overstyr beregning: {overstyrBeregning.beskrivelse}
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <ToDoWrapper>TODO - innhold kommer i EY-3006</ToDoWrapper>
    </Content>
  )
}

export default OverstyrBeregningGrunnlag

const ToDoWrapper = styled(BodyShort)`
  padding: 1em 4em;
  max-width: 70em;
  margin-bottom: 1rem;
`
