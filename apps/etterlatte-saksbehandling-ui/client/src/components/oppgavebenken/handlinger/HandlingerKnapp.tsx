import React from 'react'
import { Button } from '@navikt/ds-react'
import { handlinger, Handlinger } from '../typer/oppgavebenken'
import { useNavigate } from 'react-router-dom'

const HandlingerKnapp: React.FC<{ handling: Handlinger; behandlingsId?: string; person: string }> = ({
  handling,
  behandlingsId,
  person,
}) => {
  const navigate = useNavigate()

  const utfoerHandling = () => {
    switch (handling) {
      case Handlinger.BEHANDLE:
        navigate(`behandling/${behandlingsId}`)
        break
      case Handlinger.SE_PAA_SAK:
        navigate(`person/${person}`)
        break
    }
  }

  return (
    <Button size={'small'} onClick={utfoerHandling} variant={'secondary'}>
      {handlinger[handling]?.navn}
    </Button>
  )
}

export default HandlingerKnapp
