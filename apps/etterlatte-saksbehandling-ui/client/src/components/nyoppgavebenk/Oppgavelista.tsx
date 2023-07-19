import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { TildelSaksbehandler } from '~components/nyoppgavebenk/TildelSaksbehandler'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'
import { OppgaveDTOny, Oppgavestatus, Oppgavetype } from '~shared/api/oppgaverny'
import { Select } from '@navikt/ds-react'
import { useState } from 'react'
import styled from 'styled-components'

const SaksbehandlerFilter = {
  visAlle: 'Vis alle',
  Tildelt: 'Tildelt saksbehandler ',
  IkkeTildelt: 'Ikke tildelt saksbehandler',
}
type SaksbehandlerFilterKeys = keyof typeof SaksbehandlerFilter

function filtrerSaksbehandler(saksbehandlerFilter: SaksbehandlerFilterKeys, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (saksbehandlerFilter === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => {
      if (saksbehandlerFilter === 'Tildelt') {
        return o.saksbehandler !== null
      } else if (saksbehandlerFilter === 'IkkeTildelt') {
        return o.saksbehandler === null
      }
    })
  }
}

const EnhetFilter = {
  visAlle: 'Vis alle',
  E4815: 'Ålesund - 4815',
  E4808: 'Porsgrunn - 4808',
  E4817: 'Steinkjer - 4817',
  E4862: 'Ålesund utland - 4862',
  E0001: 'Utland - 0001',
  E4883: 'Egne ansatte - 4883',
  E2103: 'Vikafossen - 2103',
}

type EnhetFilterKeys = keyof typeof EnhetFilter

function filtrerEnhet(enhetsFilter: EnhetFilterKeys, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (enhetsFilter === 'visAlle') {
    return oppgaver
  } else {
    const enhetUtenPrefixE = enhetsFilter.substring(1)
    return oppgaver.filter((o) => o.enhet === enhetUtenPrefixE)
  }
}

const YtelseFilter = {
  visAlle: 'Vis alle',
  BARNEPENSJON: 'Barnepensjon',
  OMSTILLINGSSTOENAD: 'Omstillingsstønad',
}

type YtelseFilterKeys = keyof typeof YtelseFilter

function filtrerYtelse(ytelseFilter: YtelseFilterKeys, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (ytelseFilter === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.sakType === ytelseFilter)
  }
}

type visAlle = 'visAlle'
type OppgavestatusFilterKeys = Oppgavestatus | visAlle

const OppgavestatusFilter: Record<OppgavestatusFilterKeys, string> = {
  visAlle: 'Vis alle',
  NY: 'Ny',
  UNDER_BEHANDLING: 'Under arbeid',
  FERDIGSTILT: 'Ferdigstilt',
  FEILREGISTRERT: 'Feilregistrert',
}

function filtrerOppgaveStatus(
  oppgavestatusFilterKeys: OppgavestatusFilterKeys,
  oppgaver: OppgaveDTOny[]
): OppgaveDTOny[] {
  if (oppgavestatusFilterKeys === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.status === oppgavestatusFilterKeys)
  }
}

type OppgavetypeFilterKeys = Oppgavetype | visAlle
const OppgavetypeFilter: Record<OppgavetypeFilterKeys, string> = {
  visAlle: 'Vis alle',
  FOERSTEGANGSBEHANDLING: 'Førstegangsbehandling',
  REVUDERING: 'Revurdering',
  HENDELSE: 'Hendelse',
  MANUELT_OPPHOER: 'Manuelt opphør',
  EKSTERN: 'Ekstern',
}

function filtrerOppgaveType(oppgavetypeFilterKeys: OppgavetypeFilterKeys, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (oppgavetypeFilterKeys === 'visAlle') {
    return oppgaver
  } else {
    return oppgaver.filter((o) => o.type === oppgavetypeFilterKeys)
  }
}

function filtrerOppgaver(
  enhetsFilter: EnhetFilterKeys,
  saksbehandlerFilter: SaksbehandlerFilterKeys,
  ytelseFilter: YtelseFilterKeys,
  oppgavestatusFilter: OppgavestatusFilterKeys,
  oppgavetypeFilter: OppgavetypeFilterKeys,
  oppgaver: OppgaveDTOny[]
): OppgaveDTOny[] {
  const enhetFiltrert = filtrerEnhet(enhetsFilter, oppgaver)
  const saksbehandlerFiltrert = filtrerSaksbehandler(saksbehandlerFilter, enhetFiltrert)
  const ytelseFiltrert = filtrerYtelse(ytelseFilter, saksbehandlerFiltrert)
  const oppgaveFiltrert = filtrerOppgaveStatus(oppgavestatusFilter, ytelseFiltrert)
  const oppgaveTypeFiltrert = filtrerOppgaveType(oppgavetypeFilter, oppgaveFiltrert)

  return oppgaveTypeFiltrert
}

export const FilterFlex = styled.div`
  display: flex;
  justify-content: space-evenly;
`

export const Oppgavelista = (props: { oppgaver: ReadonlyArray<OppgaveDTOny> }) => {
  const { oppgaver } = props

  const [saksbehandlerFilter, setSaksbehandlerFilter] = useState<SaksbehandlerFilterKeys>('visAlle')
  const [enhetsFilter, setEnhetsFilter] = useState<EnhetFilterKeys>('visAlle')
  const [ytelseFilter, setYtelseFilter] = useState<YtelseFilterKeys>('visAlle')
  const [oppgavestatusFilter, setOppgavestatusFilter] = useState<OppgavestatusFilterKeys>('visAlle')
  const [oppgavetypeFilter, setOppgavetypeFilter] = useState<OppgavetypeFilterKeys>('visAlle')

  const mutableOppgaver = oppgaver.concat()
  const filtrerteOppgaver = filtrerOppgaver(
    enhetsFilter,
    saksbehandlerFilter,
    ytelseFilter,
    oppgavestatusFilter,
    oppgavetypeFilter,
    mutableOppgaver
  )

  return (
    <div>
      <FilterFlex>
        <Select
          label="Saksbehandler"
          onChange={(e) => setSaksbehandlerFilter(e.target.value as SaksbehandlerFilterKeys)}
        >
          {Object.entries(SaksbehandlerFilter).map(([key, beskrivelse]) => (
            <option key={key} value={key}>
              {beskrivelse}
            </option>
          ))}
        </Select>

        <Select label="Enhet" onChange={(e) => setEnhetsFilter(e.target.value as EnhetFilterKeys)}>
          {Object.entries(EnhetFilter).map(([enhetsnummer, enhetBeskrivelse]) => (
            <option key={enhetsnummer} value={enhetsnummer}>
              {enhetBeskrivelse}
            </option>
          ))}
        </Select>
        <Select label="Ytelse" onChange={(e) => setYtelseFilter(e.target.value as YtelseFilterKeys)}>
          {Object.entries(YtelseFilter).map(([saktype, saktypetekst]) => (
            <option key={saktype} value={saktype}>
              {saktypetekst}
            </option>
          ))}
        </Select>
        <Select
          label="Oppgavestatus"
          onChange={(e) => setOppgavestatusFilter(e.target.value as OppgavestatusFilterKeys)}
        >
          {Object.entries(OppgavestatusFilter).map(([status, statusbeskrivelse]) => (
            <option key={status} value={status}>
              {statusbeskrivelse}
            </option>
          ))}
        </Select>
        <Select label="Oppgavetype" onChange={(e) => setOppgavetypeFilter(e.target.value as OppgavetypeFilterKeys)}>
          {Object.entries(OppgavetypeFilter).map(([type, typebeskrivelse]) => (
            <option key={type} value={type}>
              {typebeskrivelse}
            </option>
          ))}
        </Select>
      </FilterFlex>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Registreringsdato</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fnr</Table.HeaderCell>
            <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
            <Table.HeaderCell scope="col">Status</Table.HeaderCell>
            <Table.HeaderCell scope="col">Merknad</Table.HeaderCell>
            <Table.HeaderCell scope="col">Enhet</Table.HeaderCell>
            <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
            <Table.HeaderCell scope="col">Ytelse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Frist</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {filtrerteOppgaver &&
            filtrerteOppgaver.map(
              ({ id, status, enhet, type, saksbehandler, opprettet, merknad, sakType, fnr, frist }) => (
                <Table.Row key={id}>
                  <Table.HeaderCell>{formaterStringDato(opprettet)}</Table.HeaderCell>
                  <Table.HeaderCell>{fnr ? fnr : 'ikke fnr, må migreres'}</Table.HeaderCell>
                  <Table.DataCell>{type}</Table.DataCell>
                  <Table.DataCell>{status}</Table.DataCell>
                  <Table.DataCell>{merknad}</Table.DataCell>
                  <Table.DataCell>{enhet}</Table.DataCell>
                  <Table.DataCell>
                    {saksbehandler ? (
                      <RedigerSaksbehandler saksbehandler={saksbehandler} oppgaveId={id} />
                    ) : (
                      <TildelSaksbehandler oppgaveId={id} />
                    )}
                  </Table.DataCell>
                  <Table.DataCell>{sakType ? sakType : 'Ingen saktype, må migreres'}</Table.DataCell>
                  <Table.DataCell>{frist ? frist : 'Ingen frist'}</Table.DataCell>
                </Table.Row>
              )
            )}
        </Table.Body>
      </Table>
    </div>
  )
}
