import { GosysBrukerType, GosysOppgaveBruker } from '~shared/types/Gosys'
import PersonOversiktLenke from '~components/oppgavebenk/components/PersonoversiktLenke'

export const GosysBrukerWrapper = ({ bruker }: { bruker?: GosysOppgaveBruker }) => {
  if (!bruker?.type || !bruker?.ident) return '-'

  switch (bruker.type) {
    case GosysBrukerType.PERSON:
      return <PersonOversiktLenke fnr={bruker.ident} />
    case GosysBrukerType.ARBEIDSGIVER:
      return `${bruker.ident} (arbeidsgiver)`
    case GosysBrukerType.SAMHANDLER:
      return `${bruker.ident} (samhandler)`
    default:
      return `${bruker?.ident} (ukjent type)`
  }
}
