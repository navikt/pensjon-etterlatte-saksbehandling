import { Select } from '@navikt/ds-react'
import { ENHETFILTER, EnhetFilterKeys, Filter } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import React from 'react'

export const FiltrerPaaEnhet = ({ filter, setFilter }: { filter: Filter; setFilter: (filter: Filter) => void }) => {
  const enhetesFilterSomListeSortertAlfabetisk = Object.entries(ENHETFILTER)

  enhetesFilterSomListeSortertAlfabetisk
    .toSorted((first, last) => {
      console.log(first[1].trim().toLowerCase())

      if (first[1].trim().toLowerCase() > last[1].trim().toLowerCase()) {
        return 1
      }
      return -1
    })
    // Dytt "Vis alle" først
    .filter((obj) => obj[0] === 'visAlle')
    .push(['visAlle', 'Vis alle'])

  return (
    <Select
      label="Enhet"
      value={filter.enhetsFilter}
      onChange={(e) => setFilter({ ...filter, enhetsFilter: e.target.value as EnhetFilterKeys })}
    >
      {enhetesFilterSomListeSortertAlfabetisk.map(([enhetsnummer, enhetBeskrivelse]) => (
        <option key={enhetsnummer} value={enhetsnummer}>
          {enhetBeskrivelse}
        </option>
      ))}
    </Select>
  )
}
