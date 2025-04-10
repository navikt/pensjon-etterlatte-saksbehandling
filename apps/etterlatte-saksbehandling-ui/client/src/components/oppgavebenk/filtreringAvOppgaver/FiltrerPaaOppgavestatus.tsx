import { Filter, OPPGAVESTATUSFILTER } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { MultiSelectFilter } from '~components/oppgavebenk/filtreringAvOppgaver/MultiSelectFilter'
import React from 'react'

export const FiltrerPaaOppgavestatus = ({
  hentAlleOppgaver,
  filter,
  setFilter,
}: {
  hentAlleOppgaver: (oppgavestatusFilter?: Array<string>) => void
  filter: Filter
  setFilter: (filter: Filter) => void
}) => {
  const oppgavestatusFilterSortertAlfabetisk = Object.entries(OPPGAVESTATUSFILTER)

  oppgavestatusFilterSortertAlfabetisk
    .toSorted((first, last) => {
      if (first[1].trim().toLowerCase() > last[1].trim().toLowerCase()) {
        return 1
      }
      return -1
    })
    // Dytt "Vis alle" først
    .filter((obj) => obj[0] === 'visAlle')
    .push(['visAlle', 'Vis alle'])

  return (
    <MultiSelectFilter
      label="Oppgavestatus"
      options={oppgavestatusFilterSortertAlfabetisk.map(([, beskrivelse]) => beskrivelse)}
      values={filter.oppgavestatusFilter}
      onChange={(statuser) => {
        const statusFilter = statuser.includes(OPPGAVESTATUSFILTER.visAlle) ? [] : statuser
        hentAlleOppgaver(statusFilter)
        setFilter({ ...filter, oppgavestatusFilter: statusFilter })
      }}
    />
  )
}
