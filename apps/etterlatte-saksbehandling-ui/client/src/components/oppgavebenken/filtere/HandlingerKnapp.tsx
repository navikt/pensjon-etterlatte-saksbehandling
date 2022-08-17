import React from 'react'
import { Button } from '@navikt/ds-react'
import { handlinger, Handlinger } from '../typer/oppgavebenken'
import { useNavigate } from 'react-router-dom'

const HandlingerKnapp: React.FC<{ handling: Handlinger; behandlingsId: string }> = ({ handling, behandlingsId }) => {
  const navigate = useNavigate()

  const goToBehandling = () => {
    navigate(`behandling/${behandlingsId}/soeknadsoversikt`)
  }

  //TODO skru på denne funksjonaliteten etter oppgavhåndteringer avklart
  //return sakErTildeltInnloggetSaksbehandler ? (
  return (
    <Button size={'small'} onClick={goToBehandling} variant={'secondary'}>
      {handlinger[handling]?.navn}
    </Button>
  )
}

export default HandlingerKnapp
