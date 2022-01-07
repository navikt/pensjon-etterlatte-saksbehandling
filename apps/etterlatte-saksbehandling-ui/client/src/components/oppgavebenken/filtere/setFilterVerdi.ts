import { IOppgaveFelt, IOppgaveFelter } from '../oppgavefelter'

export const settFilterVerdi = (
  oppgaveFelt: IOppgaveFelt,
  nyVerdi: string | Date,
  oppgaveFelter: IOppgaveFelter,
  setOppgaveFelter: (oppgaver: IOppgaveFelter) => void
) => {
  if (oppgaveFelt.filter) {
    const oppdaterteOppgaveFelter = {
      ...oppgaveFelter,
      [oppgaveFelt.noekkel]: {
        ...oppgaveFelt,
        filter: {
          ...oppgaveFelt.filter,
          selectedValue: nyVerdi,
        },
      },
    }
    setOppgaveFelter(oppdaterteOppgaveFelter)
  }
}
