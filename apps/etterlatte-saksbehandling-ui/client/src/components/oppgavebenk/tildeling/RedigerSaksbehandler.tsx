import { Oppgavetype } from '~shared/api/oppgaver'
import React from 'react'
import { useAppSelector } from '~store/Store'
import { FjernSaksbehandler } from '~components/oppgavebenk/tildeling/FjernSaksbehandler'
import { ByttSaksbehandler } from './ByttSaksbehandler'
import { TildelSaksbehandler } from './TildelSaksbehandler'

export interface RedigerSaksbehandlerProps {
  saksbehandler: string | null
  oppgaveId: string
  sakId: number
  oppdaterTildeling: (id: string, saksbehandler: string | null, versjon: number | null) => void
  erRedigerbar: boolean
  versjon: number | null
  type: Oppgavetype
}

export const RedigerSaksbehandler = (props: RedigerSaksbehandlerProps) => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const brukerErSaksbehandler = innloggetSaksbehandler.ident === props.saksbehandler

  if (!props.saksbehandler) return <TildelSaksbehandler {...props} />
  if (brukerErSaksbehandler) return <FjernSaksbehandler {...props} />
  else return <ByttSaksbehandler {...props} />
}
