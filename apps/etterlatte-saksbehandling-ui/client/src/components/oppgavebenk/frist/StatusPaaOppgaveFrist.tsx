import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { isBefore } from 'date-fns'
import { BodyShort, ErrorMessage } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'

export const StatusPaaOppgaveFrist = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const visAtFristHarPassert =
    isBefore(new Date(oppgave.frist), new Date()) && oppgave.status !== Oppgavestatus.FERDIGSTILT

  return visAtFristHarPassert ? (
    <ErrorMessage>{formaterDato(oppgave.frist)}</ErrorMessage>
  ) : (
    <BodyShort>{formaterDato(oppgave.frist)}</BodyShort>
  )
}
