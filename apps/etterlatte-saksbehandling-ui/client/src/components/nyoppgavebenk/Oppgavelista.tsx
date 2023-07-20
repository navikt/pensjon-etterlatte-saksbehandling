import { Button, Select, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { TildelSaksbehandler } from '~components/nyoppgavebenk/TildelSaksbehandler'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'
import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { useState } from 'react'
import styled from 'styled-components'
import {
  EnhetFilterKeys,
  filtrerOppgaver,
  OppgavestatusFilter,
  OppgavestatusFilterKeys,
  OppgavetypeFilter,
  OppgavetypeFilterKeys,
  SaksbehandlerFilter,
  SaksbehandlerFilterKeys,
  YtelseFilter,
  YtelseFilterKeys,
  EnhetFilter,
} from '~components/nyoppgavebenk/Oppgavelistafiltre'

const FilterFlex = styled.div`
  display: flex;
  justify-content: space-evenly;
`

const ButtonWrapper = styled.div`
  display: flex;
  justify-content: flex-start;
  margin: 2rem 2rem 2rem 0rem;
  max-width: 20em;
  button:first-child {
    margin-right: 1rem;
  }
`

export const Oppgavelista = (props: { oppgaver: ReadonlyArray<OppgaveDTOny>; hentOppgaver: () => void }) => {
  const { oppgaver, hentOppgaver } = props

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
    <>
      <FilterFlex>
        <Select
          label="Saksbehandler"
          value={saksbehandlerFilter}
          onChange={(e) => setSaksbehandlerFilter(e.target.value as SaksbehandlerFilterKeys)}
        >
          {Object.entries(SaksbehandlerFilter).map(([key, beskrivelse]) => (
            <option key={key} value={key}>
              {beskrivelse}
            </option>
          ))}
        </Select>

        <Select label="Enhet" value={enhetsFilter} onChange={(e) => setEnhetsFilter(e.target.value as EnhetFilterKeys)}>
          {Object.entries(EnhetFilter).map(([enhetsnummer, enhetBeskrivelse]) => (
            <option key={enhetsnummer} value={enhetsnummer}>
              {enhetBeskrivelse}
            </option>
          ))}
        </Select>
        <Select
          label="Ytelse"
          value={ytelseFilter}
          onChange={(e) => setYtelseFilter(e.target.value as YtelseFilterKeys)}
        >
          {Object.entries(YtelseFilter).map(([saktype, saktypetekst]) => (
            <option key={saktype} value={saktype}>
              {saktypetekst}
            </option>
          ))}
        </Select>
        <Select
          label="Oppgavestatus"
          value={oppgavestatusFilter}
          onChange={(e) => setOppgavestatusFilter(e.target.value as OppgavestatusFilterKeys)}
        >
          {Object.entries(OppgavestatusFilter).map(([status, statusbeskrivelse]) => (
            <option key={status} value={status}>
              {statusbeskrivelse}
            </option>
          ))}
        </Select>
        <Select
          label="Oppgavetype"
          value={oppgavetypeFilter}
          onChange={(e) => setOppgavetypeFilter(e.target.value as OppgavetypeFilterKeys)}
        >
          {Object.entries(OppgavetypeFilter).map(([type, typebeskrivelse]) => (
            <option key={type} value={type}>
              {typebeskrivelse}
            </option>
          ))}
        </Select>
      </FilterFlex>
      <ButtonWrapper>
        <Button onClick={hentOppgaver}>Hent</Button>
        <Button
          variant="secondary"
          onClick={() => {
            setSaksbehandlerFilter('visAlle')
            setOppgavetypeFilter('visAlle')
            setEnhetsFilter('visAlle')
            setOppgavestatusFilter('visAlle')
            setSaksbehandlerFilter('visAlle')
          }}
        >
          Tilbakestill alle filtre
        </Button>
      </ButtonWrapper>
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
    </>
  )
}
