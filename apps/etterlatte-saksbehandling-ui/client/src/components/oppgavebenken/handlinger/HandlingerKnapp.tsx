import React from 'react'
import { Button } from '@navikt/ds-react'
import { handlinger, Handlinger } from '../typer/oppgavebenken'
import styled from 'styled-components'

const HandlingerKnapp: React.FC<{ handling: Handlinger; behandlingsId?: string; person: string }> = ({
  handling,
  behandlingsId,
  person,
}) => {
  const destinasjon = handling === Handlinger.BEHANDLE ? `behandling/${behandlingsId}` : `person/${person}`
  return (
    <Button as={'a'} href={destinasjon} size={'small'} variant={'primary'}>
      <ActionText>{handlinger[handling]?.navn}</ActionText>
    </Button>
  )
}

const ActionText = styled.div`
  white-space: nowrap;
`

export default HandlingerKnapp
