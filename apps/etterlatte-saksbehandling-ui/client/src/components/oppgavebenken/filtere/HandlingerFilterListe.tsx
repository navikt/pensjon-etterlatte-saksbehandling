import React, { useContext } from 'react'
import { Button } from '@navikt/ds-react'
import { AppContext } from '../../../store/AppContext'
import { handlinger, Handlinger } from '../typer/oppgavebenken'

const HandlingerFilterListe: React.FC<{ saksbehandler: string; handling: Handlinger }> = ({
  saksbehandler,
  handling,
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

export default HandlingerFilterListe
