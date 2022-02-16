import React, { useContext } from 'react'
import { Button } from '@navikt/ds-react'
import { AppContext } from '../../../store/AppContext'
import { handlinger, Handlinger } from '../typer/oppgavebenken'

const HandlingerKnapp: React.FC<{ saksbehandler: string; handling: Handlinger; behandlingsId: string }> = ({
  saksbehandler,
  handling,
  behandlingsId,
}) => {
  const innloggetSaksbehandler = useContext(AppContext).state.saksbehandlerReducer.navn
  const sakErTildeltInnloggetSaksbehandler = innloggetSaksbehandler === saksbehandler

  //TODO skru på denne funksjonaliteten etter oppgavhåndteringer avklart
  //return sakErTildeltInnloggetSaksbehandler ? (
  return (
    <Button size={'small'} onClick={() => {}} variant={'secondary'}>
      {handlinger[handling]?.navn}
    </Button>
  )
}

export default HandlingerKnapp
