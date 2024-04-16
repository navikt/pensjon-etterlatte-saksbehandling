import { Button, Select, TextField } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import { FlexRow } from '~shared/styled'
import {
  ENHETFILTER,
  EnhetFilterKeys,
  GOSYS_TEMA_FILTER,
  GosysFilter,
} from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { ArrowCirclepathIcon, ArrowUndoIcon } from '@navikt/aksel-icons'
import { GosysTema } from '~shared/types/Gosys'

interface Props {
  hentAlleOppgaver: () => void
  filter: GosysFilter
  setFilter: (filter: GosysFilter) => void
  filterFoedselsnummer: (fnr?: string) => void
}

export const GosysFilterRad = ({ hentAlleOppgaver, filter, setFilter, filterFoedselsnummer }: Props): ReactNode => {
  const [fnr, setFnr] = useState<string>()

  useEffect(() => {
    const delay = setTimeout(() => filterFoedselsnummer(fnr), 500)
    return () => clearTimeout(delay)
  }, [fnr])

  return (
    <>
      <FlexRow $spacing align="start">
        <TextField
          label="Fødselsnummer"
          width="1rem"
          value={fnr || ''}
          onChange={(e) => setFnr(e.target.value?.replace(/[^0-9+]/, ''))}
          type="tel"
          min={0}
          placeholder="Søk"
        />

        <Select
          label="Enhet"
          value={filter.enhetFilter}
          onChange={(e) => setFilter({ ...filter, enhetFilter: e.target.value as EnhetFilterKeys })}
        >
          {Object.entries(ENHETFILTER).map(([enhetsnummer, enhetBeskrivelse]) => (
            <option key={enhetsnummer} value={enhetsnummer}>
              {enhetBeskrivelse}
            </option>
          ))}
        </Select>

        <Select
          label="Tema"
          value={filter.temaFilter}
          onChange={(e) => setFilter({ ...filter, temaFilter: e.target.value as GosysTema })}
        >
          <option value="">Barnepensjon og omstillingsstønad</option>
          {Object.entries(GOSYS_TEMA_FILTER).map(([tema, beskrivelse]) => (
            <option key={tema} value={tema}>
              {beskrivelse}
            </option>
          ))}
        </Select>
      </FlexRow>

      <FlexRow $spacing>
        <Button onClick={() => hentAlleOppgaver()} size="small" icon={<ArrowCirclepathIcon />} iconPosition="right">
          Hent oppgaver
        </Button>
        <Button
          variant="secondary"
          onClick={() => {
            setFilter({})
            hentAlleOppgaver()
          }}
          size="small"
          icon={<ArrowUndoIcon />}
          iconPosition="right"
        >
          Tilbakestill filtre
        </Button>
      </FlexRow>
    </>
  )
}
