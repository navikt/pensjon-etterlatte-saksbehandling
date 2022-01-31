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

  return sakErTildeltInnloggetSaksbehandler ? (
    <Button size={'small'} onClick={() => {}} variant={'secondary'}>
      {handlinger[handling]?.navn}
    </Button>
  ) : (
    <div></div>
  )
}

export default HandlingerFilterListe
