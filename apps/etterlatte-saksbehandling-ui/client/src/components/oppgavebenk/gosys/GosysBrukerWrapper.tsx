import { GosysBrukerType, GosysOppgaveBruker } from '~shared/types/Gosys'

export const GosysBrukerWrapper = ({ bruker }: { bruker?: GosysOppgaveBruker }) => {
  if (!bruker?.type || !bruker?.ident) return '-'

  switch (bruker.type) {
    case GosysBrukerType.PERSON:
      return `${bruker.ident}`
    case GosysBrukerType.ARBEIDSGIVER:
      return `${bruker.ident} (arbeidsgiver)`
    case GosysBrukerType.SAMHANDLER:
      return `${bruker.ident} (samhandler)`
    default:
      return `${bruker?.ident} (ukjent type)`
  }
}
