import React from 'react'
import { Button } from '@navikt/ds-react'
import { handlinger, Handlinger } from '../typer/oppgavebenken'
import styled from 'styled-components'

const HandlingerKnapp: React.FC<{ handling: Handlinger; behandlingsId: string }> = ({ handling, behandlingsId }) => {
  //TODO skru på denne funksjonaliteten etter oppgavhåndteringer avklart
  //return sakErTildeltInnloggetSaksbehandler ? (
  return (
    <Button as={'a'} href={`behandling/${behandlingsId}`} size={'small'} variant={'primary'}>
      <ActionText>{handlinger[handling]?.navn}</ActionText>
    </Button>
  )
}

const ActionText = styled.div`
  white-space: nowrap;
`

export default HandlingerKnapp
