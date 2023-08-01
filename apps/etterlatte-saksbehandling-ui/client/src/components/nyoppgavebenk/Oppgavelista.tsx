import { Button, Pagination, Select, Table, TextField } from '@navikt/ds-react'
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
  OppgaveKildeFilterKeys,
  OPPGAVEKILDEFILTER,
  FRISTFILTER,
  FristFilterKeys,
} from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { HandlingerForOppgave } from '~components/nyoppgavebenk/HandlingerForOppgave'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { isBefore } from 'date-fns'
import SaksoversiktLenke from '~components/oppgavebenken/handlinger/BrukeroversiktKnapp'

const FilterFlex = styled.div`
  display: flex;
  gap: 2rem;
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

export const FristWrapper = styled.span<{ fristHarPassert: boolean }>`
  color: ${(p) => p.fristHarPassert && 'var(--a-text-danger)'};
`

export const Oppgavelista = (props: { oppgaver: ReadonlyArray<OppgaveDTOny>; hentOppgaver: () => void }) => {
  const { oppgaver, hentOppgaver } = props

  const [saksbehandlerFilter, setSaksbehandlerFilter] = useState<SaksbehandlerFilterKeys>('IkkeTildelt')
  const [fristFilter, setFristFilter] = useState<FristFilterKeys>('visAlle')
  const [enhetsFilter, setEnhetsFilter] = useState<EnhetFilterKeys>('visAlle')
  const [ytelseFilter, setYtelseFilter] = useState<YtelseFilterKeys>('visAlle')
  const [oppgavestatusFilter, setOppgavestatusFilter] = useState<OppgavestatusFilterKeys>('visAlle')
  const [oppgavetypeFilter, setOppgavetypeFilter] = useState<OppgavetypeFilterKeys>('visAlle')
  const [oppgavekildeFilter, setOppgavekildeFilter] = useState<OppgaveKildeFilterKeys>('visAlle')
  const [fnrFilter, setFnrFilter] = useState<string>('')
  const [page, setPage] = useState<number>(1)
  const rowsPerPage = 10
  const mutableOppgaver = oppgaver.concat()
  const filtrerteOppgaver = filtrerOppgaver(
    enhetsFilter,
    fristFilter,
    saksbehandlerFilter,
    ytelseFilter,
    oppgavestatusFilter,
    oppgavetypeFilter,
    oppgavekildeFilter,
    mutableOppgaver,
    fnrFilter
  )

  let paginerteOppgaver = filtrerteOppgaver
  paginerteOppgaver = paginerteOppgaver.slice((page - 1) * rowsPerPage, page * rowsPerPage)

  return (
    <>
      <FilterFlex>
        <TextField
          label="Fødselsnummer"
          value={fnrFilter}
          onChange={(e) => setFnrFilter(e.target.value)}
          placeholder={'Søk'}
          autoComplete="off"
        />
        <Select label="Frist" value={fristFilter} onChange={(e) => setFristFilter(e.target.value as FristFilterKeys)}>
          {Object.entries(FRISTFILTER).map(([key, fristBeskrivelse]) => (
            <option key={key} value={key}>
              {fristBeskrivelse}
            </option>
          ))}
        </Select>
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
        <Select
          label="Kilde"
          value={oppgavekildeFilter}
          onChange={(e) => setOppgavekildeFilter(e.target.value as OppgaveKildeFilterKeys)}
        >
          {Object.entries(OPPGAVEKILDEFILTER).map(([type, typebeskrivelse]) => (
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
            setOppgavekildeFilter('visAlle')
            setFnrFilter('')
          }}
        >
          Tilbakestill alle filtre
        </Button>
      </ButtonWrapper>
      {paginerteOppgaver && paginerteOppgaver.length > 0 ? (
        <>
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell scope="col">Registreringsdato</Table.HeaderCell>
                <Table.HeaderCell scope="col">Frist</Table.HeaderCell>
                <Table.HeaderCell scope="col">Fnr</Table.HeaderCell>
                <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
                <Table.HeaderCell scope="col">Status</Table.HeaderCell>
                <Table.HeaderCell scope="col">Merknad</Table.HeaderCell>
                <Table.HeaderCell scope="col">Enhet</Table.HeaderCell>
                <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
                <Table.HeaderCell scope="col">Ytelse</Table.HeaderCell>
                <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {paginerteOppgaver &&
                paginerteOppgaver.map(
                  ({
                    id,
                    status,
                    enhet,
                    type,
                    saksbehandler,
                    opprettet,
                    merknad,
                    sakType,
                    fnr,
                    frist,
                    sakId,
                    referanse,
                  }) => (
                    <Table.Row key={id}>
                      <Table.HeaderCell>{formaterStringDato(opprettet)}</Table.HeaderCell>
                      <Table.DataCell>
                        <FristWrapper fristHarPassert={!!frist && isBefore(new Date(frist), new Date())}>
                          {frist ? formaterStringDato(frist) : 'Ingen frist'}
                        </FristWrapper>
                      </Table.DataCell>
                      <Table.HeaderCell>
                        <SaksoversiktLenke fnr={fnr} />
                      </Table.HeaderCell>
                      <Table.DataCell>
                        {type ? <OppgavetypeTag oppgavetype={type} /> : <div>oppgaeveid {id}</div>}
                      </Table.DataCell>
                      <Table.DataCell>{status}</Table.DataCell>
                      <Table.DataCell>{merknad}</Table.DataCell>
                      <Table.DataCell>{enhet}</Table.DataCell>
                      <Table.DataCell>
                        {saksbehandler ? (
                          <RedigerSaksbehandler saksbehandler={saksbehandler} oppgaveId={id} sakId={sakId} />
                        ) : (
                          <TildelSaksbehandler oppgaveId={id} sakId={sakId} />
                        )}
                      </Table.DataCell>
                      <Table.DataCell>{sakType && <SaktypeTag sakType={sakType} />}</Table.DataCell>
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
      ) : (
        <>Ingen oppgaver</>
      )}
    </>
  )
}
