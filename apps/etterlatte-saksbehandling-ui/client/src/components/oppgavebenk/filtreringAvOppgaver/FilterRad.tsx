import { Button, HStack, Select, TextField, VStack } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import {
  initialFilter,
  initialMinOppgavelisteFiltre,
} from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { FiltrerPaaSaksbehandler } from '~components/oppgavebenk/filtreringAvOppgaver/FiltrerPaaSaksbehandler'
import {
  ENHETFILTER,
  EnhetFilterKeys,
  Filter,
  FRISTFILTER,
  FristFilterKeys,
  OPPGAVESTATUSFILTER,
  OPPGAVETYPEFILTER,
  YTELSEFILTER,
  YtelseFilterKeys,
} from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { MultiSelectFilter } from '~components/oppgavebenk/filtreringAvOppgaver/MultiSelectFilter'
import { ArrowCirclepathIcon, ArrowUndoIcon } from '@navikt/aksel-icons'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'

interface Props {
  hentAlleOppgaver: (oppgavestatusFilter?: Array<string>) => void
  filter: Filter
  setFilter: (filter: Filter) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppgavelisteValg?: OppgavelisteValg
  children?: ReactNode
}

export const FilterRad = ({
  hentAlleOppgaver,
  filter,
  setFilter,
  saksbehandlereIEnhet,
  oppgavelisteValg,
  children,
}: Props): ReactNode => {
  const [sakEllerFnr, setSakEllerFnr] = useState<string>(filter.sakEllerFnrFilter)

  useEffect(() => {
    const delay = setTimeout(() => setFilter({ ...filter, sakEllerFnrFilter: sakEllerFnr }), 500)
    return () => clearTimeout(delay)
  }, [sakEllerFnr])

  const oppgaveTypeOptions = Object.entries(OPPGAVETYPEFILTER)
    .map(([, beskrivelse]) => beskrivelse)
    .sort((a, b) => a.localeCompare(b))

  return (
    <VStack gap="space-16">
      <HStack gap="space-4" align="start" wrap={false}>
        <TextField
          label="Sakid / Fnr."
          width="1rem"
          value={sakEllerFnr}
          onChange={(e) => setSakEllerFnr(e.target.value?.replace(/[^0-9+]/, ''))}
          type="tel"
          min={0}
          placeholder="Søk"
        />
        <Select
          label="Frist"
          value={filter.fristFilter}
          onChange={(e) =>
            setFilter({
              ...filter,
              fristFilter: e.target.value as FristFilterKeys,
            })
          }
        >
          {Object.entries(FRISTFILTER)
            .filter((o) => o)
            .map(([key, fristBeskrivelse]) => (
              <option key={key} value={key}>
                {fristBeskrivelse}
              </option>
            ))}
        </Select>
        {oppgavelisteValg === OppgavelisteValg.OPPGAVELISTA && (
          <FiltrerPaaSaksbehandler saksbehandlereIEnhet={saksbehandlereIEnhet} filter={filter} setFilter={setFilter} />
        )}
        {oppgavelisteValg !== OppgavelisteValg.MIN_OPPGAVELISTE && (
          <Select
            label="Enhet"
            value={filter.enhetsFilter}
            onChange={(e) => setFilter({ ...filter, enhetsFilter: e.target.value as EnhetFilterKeys })}
          >
            {Object.entries(ENHETFILTER).map(([enhetsnummer, enhetBeskrivelse]) => (
              <option key={enhetsnummer} value={enhetsnummer}>
                {enhetBeskrivelse}
              </option>
            ))}
          </Select>
        )}

        <Select
          label="Ytelse"
          value={filter.ytelseFilter}
          onChange={(e) => setFilter({ ...filter, ytelseFilter: e.target.value as YtelseFilterKeys })}
        >
          {Object.entries(YTELSEFILTER).map(([saktype, saktypetekst]) => (
            <option key={saktype} value={saktype}>
              {saktypetekst}
            </option>
          ))}
        </Select>

        {oppgavelisteValg !== OppgavelisteValg.GOSYS_OPPGAVER && (
          <>
            <MultiSelectFilter
              label="Oppgavestatus"
              options={Object.entries(OPPGAVESTATUSFILTER).map(([, beskrivelse]) => beskrivelse)}
              values={filter.oppgavestatusFilter}
              onChange={(statuser) => {
                const statusFilter = statuser.includes(OPPGAVESTATUSFILTER.visAlle) ? [] : statuser
                hentAlleOppgaver(statusFilter)
                setFilter({ ...filter, oppgavestatusFilter: statusFilter })
              }}
            />

            <MultiSelectFilter
              label="Oppgavetype"
              options={oppgaveTypeOptions}
              values={filter.oppgavetypeFilter}
              onChange={(typer) =>
                setFilter({ ...filter, oppgavetypeFilter: typer.includes(OPPGAVETYPEFILTER.visAlle) ? [] : typer })
              }
            />
          </>
        )}

        {children}
      </HStack>
      <HStack gap="space-4">
        <Button
          onClick={() => hentAlleOppgaver()}
          size="small"
          icon={<ArrowCirclepathIcon aria-hidden />}
          iconPosition="right"
        >
          Hent oppgaver
        </Button>
        <Button
          variant="secondary"
          onClick={() => {
            const filter =
              oppgavelisteValg === OppgavelisteValg.OPPGAVELISTA ? initialFilter() : initialMinOppgavelisteFiltre()
            hentAlleOppgaver(filter.oppgavestatusFilter)
            setFilter(filter)
          }}
          size="small"
          icon={<ArrowUndoIcon aria-hidden />}
          iconPosition="right"
        >
          Tilbakestill filtre
        </Button>
      </HStack>
    </VStack>
  )
}
