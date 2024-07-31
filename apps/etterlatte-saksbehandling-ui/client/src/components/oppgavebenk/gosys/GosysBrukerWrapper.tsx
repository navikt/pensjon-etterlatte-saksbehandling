import { GosysBrukerType, GosysOppgaveBruker } from '~shared/types/Gosys'
import SaksoversiktLenke from '~components/oppgavebenk/components/SaksoversiktLenke'
import React from 'react'

export const GosysBrukerWrapper = ({ bruker }: { bruker?: GosysOppgaveBruker }) => {
  if (!bruker?.type || !bruker?.ident) return '-'

  switch (bruker.type) {
    case GosysBrukerType.PERSON:
      return <SaksoversiktLenke sakId={bruker.ident} />
    case GosysBrukerType.ARBEIDSGIVER:
      return `${bruker.ident} (arbeidsgiver)`
    case GosysBrukerType.SAMHANDLER:
      return `${bruker.ident} (samhandler)`
    default:
      return `${bruker?.ident} (ukjent type)`
  }
}
