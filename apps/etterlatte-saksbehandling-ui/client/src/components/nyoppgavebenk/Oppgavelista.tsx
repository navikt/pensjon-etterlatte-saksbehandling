import { Button, Pagination, Select, Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { TildelSaksbehandler } from '~components/nyoppgavebenk/TildelSaksbehandler'
import { RedigerSaksbehandler } from '~components/nyoppgavebenk/RedigerSaksbehandler'
import { OppgaveDTOny } from '~shared/api/oppgaverny'
import { useState } from 'react'
import styled from 'styled-components'
import {
  EnhetFilterKeys,
  filtrerOppgaver,
  OPPGAVESTATUSFILTER,
  OppgavestatusFilterKeys,
  OPPGAVETYPEFILTER,
  OppgavetypeFilterKeys,
  SAKSBEHANDLERFILTER,
  SaksbehandlerFilterKeys,
  YTELSEFILTER,
  YtelseFilterKeys,
  ENHETFILTER,
} from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'

const FilterFlex = styled.div`
  display: flex;
  justify-content: space-evenly;
`

const ButtonWrapper = styled.div`
  display: flex;
  justify-content: flex-start;
  margin: 2rem 2rem 2rem 0rem;
  max-width: 20rem;
  button:first-child {
    margin-right: 1rem;
  }
`

export const Oppgavelista = (props: { oppgaver: ReadonlyArray<OppgaveDTOny>; hentOppgaver: () => void }) => {
  const { oppgaver, hentOppgaver } = props

  const [saksbehandlerFilter, setSaksbehandlerFilter] = useState<SaksbehandlerFilterKeys>('IkkeTildelt')
  const [enhetsFilter, setEnhetsFilter] = useState<EnhetFilterKeys>('visAlle')
  const [ytelseFilter, setYtelseFilter] = useState<YtelseFilterKeys>('visAlle')
  const [oppgavestatusFilter, setOppgavestatusFilter] = useState<OppgavestatusFilterKeys>('visAlle')
  const [oppgavetypeFilter, setOppgavetypeFilter] = useState<OppgavetypeFilterKeys>('visAlle')
  const [page, setPage] = useState<number>(1)
  const rowsPerPage = 10
  const mutableOppgaver = oppgaver.concat()
  const filtrerteOppgaver = filtrerOppgaver(
    enhetsFilter,
    saksbehandlerFilter,
    ytelseFilter,
    oppgavestatusFilter,
    oppgavetypeFilter,
    mutableOppgaver
  )

  let paginerteOppgaver = filtrerteOppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  return (
    <>
      <FilterFlex>
        <Select
          label="Saksbehandler"
          value={saksbehandlerFilter}
          onChange={(e) => setSaksbehandlerFilter(e.target.value as SaksbehandlerFilterKeys)}
        >
          {Object.entries(SAKSBEHANDLERFILTER).map(([key, beskrivelse]) => (
            <option key={key} value={key}>
              {beskrivelse}
            </option>
          ))}
        </Select>

        <Select label="Enhet" value={enhetsFilter} onChange={(e) => setEnhetsFilter(e.target.value as EnhetFilterKeys)}>
          {Object.entries(ENHETFILTER).map(([enhetsnummer, enhetBeskrivelse]) => (
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
          {Object.entries(YTELSEFILTER).map(([saktype, saktypetekst]) => (
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
          {Object.entries(OPPGAVESTATUSFILTER).map(([status, statusbeskrivelse]) => (
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
          {Object.entries(OPPGAVETYPEFILTER).map(([type, typebeskrivelse]) => (
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
            <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {paginerteOppgaver &&
            paginerteOppgaver.map(
              ({ id, status, enhet, type, saksbehandler, opprettet, merknad, sakType, fnr, frist, referanse }) => (
                <Table.Row key={id}>
                  <Table.HeaderCell>{formaterStringDato(opprettet)}</Table.HeaderCell>
                  <Table.HeaderCell>{fnr}</Table.HeaderCell>
                  <Table.DataCell>
                    <OppgavetypeTag oppgavetype={type} />
                  </Table.DataCell>
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
                  <Table.DataCell>{sakType && <SaktypeTag sakType={sakType} />}</Table.DataCell>
                  <Table.DataCell>{frist ? frist : 'Ingen frist'}</Table.DataCell>
                  <Table.DataCell>
                    <HandlingerForOppgave
                      oppgavetype={type}
                      fnr={fnr}
                      saksbehandler={saksbehandler}
                      referanse={referanse}
                    />
                  </Table.DataCell>
                </Table.Row>
              )
            )}
        </Table.Body>
      </Table>
      <Pagination
        page={page}
        onPageChange={setPage}
        count={Math.ceil(filtrerteOppgaver.length / rowsPerPage)}
        size="small"
      />
    </>
  )
}
