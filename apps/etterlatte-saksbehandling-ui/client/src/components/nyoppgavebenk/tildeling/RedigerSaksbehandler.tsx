import { Oppgavetype } from '~shared/api/oppgaverny'
import React from 'react'
import { useAppSelector } from '~store/Store'
import { FjernSaksbehandler } from '~components/nyoppgavebenk/tildeling/FjernSaksbehandler'
import { ByttSaksbehandler } from './ByttSaksbehandler'
import { TildelSaksbehandler } from './TildelSaksbehandler'

export interface RedigerSaksbehandlerProps {
  saksbehandler: string | null
  oppgaveId: string
  sakId: number
  oppdaterTildeling: (id: string, saksbehandler: string | null) => void
  erRedigerbar: boolean
  versjon: string | null
  type: Oppgavetype
}

export const RedigerSaksbehandler = (props: RedigerSaksbehandlerProps) => {
  const user = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  const brukerErSaksbehandler = user.ident === props.saksbehandler

  if (!props.saksbehandler) return <TildelSaksbehandler {...props} />
  if (brukerErSaksbehandler) return <FjernSaksbehandler {...props} />
  else return <ByttSaksbehandler {...props} />
}
