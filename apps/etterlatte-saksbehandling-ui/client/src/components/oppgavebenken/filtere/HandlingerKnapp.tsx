import React, { useContext } from 'react'
import { Button } from '@navikt/ds-react'
import { AppContext } from '../../../store/AppContext'
import { handlinger, Handlinger } from '../typer/oppgavebenken'
import { useNavigate } from 'react-router-dom'

const HandlingerKnapp: React.FC<{ saksbehandler: string; handling: Handlinger; behandlingsId: string }> = ({
  saksbehandler,
  handling,
  behandlingsId,
}) => {
  const navigate = useNavigate()
  const innloggetSaksbehandler = useContext(AppContext).state.saksbehandlerReducer.navn
  const sakErTildeltInnloggetSaksbehandler = innloggetSaksbehandler === saksbehandler
  console.log(sakErTildeltInnloggetSaksbehandler)

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
