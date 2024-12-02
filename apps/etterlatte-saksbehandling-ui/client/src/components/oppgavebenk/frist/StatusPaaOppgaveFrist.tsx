import { erOppgaveRedigerbar, Oppgavestatus } from '~shared/types/oppgave'
import { isBefore } from 'date-fns'
import { BodyShort, ErrorMessage } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'

export const StatusPaaOppgaveFrist = ({
  oppgaveFrist,
  oppgaveStatus,
}: {
  oppgaveFrist: string
  oppgaveStatus: Oppgavestatus | string
}) => {
  const visAtFristHarPassert =
    isBefore(new Date(oppgaveFrist), new Date()) && !erOppgaveRedigerbar(oppgaveStatus as Oppgavestatus)

  return visAtFristHarPassert ? (
    <ErrorMessage>{formaterDato(oppgaveFrist)}</ErrorMessage>
  ) : (
    <BodyShort>{formaterDato(oppgaveFrist)}</BodyShort>
  )
}
