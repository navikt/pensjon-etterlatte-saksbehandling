import { GosysBrukerType, GosysOppgaveBruker } from '~shared/types/Gosys'
import React from 'react'
import { PersonLink } from '~components/person/PersonLink'

export const GosysBrukerWrapper = ({ bruker }: { bruker?: GosysOppgaveBruker }) => {
  if (!bruker?.type || !bruker?.ident) return '-'

  switch (bruker.type) {
    case GosysBrukerType.PERSON:
      return <PersonLink fnr={bruker.ident} />
    case GosysBrukerType.ARBEIDSGIVER:
      return `${bruker.ident} (arbeidsgiver)`
    case GosysBrukerType.SAMHANDLER:
      return `${bruker.ident} (samhandler)`
    default:
      return `${bruker?.ident} (ukjent type)`
  }
}
