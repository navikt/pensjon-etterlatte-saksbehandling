import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { TildelSaksbehandler } from '~components/nyoppgavebenk/TildelSaksbehandler'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'
import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { Select } from '@navikt/ds-react'
import { useState } from 'react'
import styled from 'styled-components'

type SaksbehandlerFilter = 'Alle' | 'Tildelt' | 'IkkeTildelt'

function filtrerSaksbehandler(saksbehandlerFilter: SaksbehandlerFilter, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (saksbehandlerFilter === 'Alle') {
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
  visAlle: 'Vis Alle',
  E4815: 'Ålesund',
  E4808: 'Porsgrunn',
  E4817: 'Steinkjer',
  E4862: 'Ålesund utland',
  E0001: 'Utland',
  E4883: 'Egne ansatte',
  E2103: 'Vikafossen',
}
type enhetKeyTypes = keyof typeof EnhetFilter

function filtrerEnhet(enhetsFilter: enhetKeyTypes, oppgaver: OppgaveDTOny[]): OppgaveDTOny[] {
  if (enhetsFilter === 'visAlle') {
    return oppgaver
  } else {
    const utenE = enhetsFilter.substring(1)
    return oppgaver.filter((o) => o.enhet === utenE)
  }
}

export const FilterFlex = styled.div`
  display: flex;
  justify-content: space-evenly;
`

export const Oppgavelista = (props: { oppgaver: ReadonlyArray<OppgaveDTOny> }) => {
  const { oppgaver } = props

  const [saksbehandlerFilter, setSaksbehandlerFilter] = useState<SaksbehandlerFilter>('Alle')
  const [enhetsFilter, setEnhetsFilter] = useState<enhetKeyTypes>('visAlle')
  const mutableOppgaver = oppgaver.concat()

  const filtrerteOppgaver = filtrerEnhet(enhetsFilter, filtrerSaksbehandler(saksbehandlerFilter, mutableOppgaver))
  return (
    <div>
      <FilterFlex>
        <Select label="Saksbehandler" onChange={(e) => setSaksbehandlerFilter(e.target.value as SaksbehandlerFilter)}>
          <option value="Alle">Vis alle</option>
          <option value="Tildelt">Tildelt saksbehandler</option>
          <option value="IkkeTildelt">Ikke tildelt saksbehandler</option>
        </Select>

        <Select label="Enhet" onChange={(e) => setEnhetsFilter(e.target.value as enhetKeyTypes)}>
          {Object.entries(EnhetFilter).map(([enhetsnummer, enhetBeskrivelse]) => (
            <option key={enhetsnummer} value={enhetsnummer}>
              {enhetBeskrivelse}
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
