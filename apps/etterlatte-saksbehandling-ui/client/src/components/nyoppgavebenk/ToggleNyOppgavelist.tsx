import styled from 'styled-components'
import { ToggleMinOppgaveliste } from '~components/nyoppgavebenk/ToggleMinOppgaveliste'

const OppgavebenkContainer = styled.div`
  padding: 2rem;
`

export const OppgavelistaContainer = () => {
  return (
    <OppgavebenkContainer>
      <ToggleMinOppgaveliste />
    </OppgavebenkContainer>
  )
}
