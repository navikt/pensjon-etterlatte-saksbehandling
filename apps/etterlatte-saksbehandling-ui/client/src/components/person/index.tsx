import { useEffect, useRef, useState } from 'react'
import { Tab, Tabs, TabList, TabPanel } from 'react-tabs'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { getPerson, opprettBehandlingPaaSak } from '../../shared/api/person'
import { StatusBar, StatusBarTheme } from '../statusbar'
import { Container } from '../../shared/styled'
import { Dokumentoversikt } from './dokumentoversikt'
import { Saksoversikt } from './saksoversikt'
import { useNavigate } from 'react-router-dom'
import { Dokumenter, PersonInfo, SakslisteProps } from './typer'

//todo: typer
const testDokumenter: Dokumenter = {
  brev: [{
    dato: 'Mock 13.05.2021', tittel: 'Mock Innvilgelsesbrev barnepensjon', link: 'link', status: 'Mock Sendt ut',
  }, {
    dato: 'Mock 09.05.2021',
    tittel: 'Mock Søknad barnepensjon - førstegangsbehandling',
    link: 'link',
    status: 'Mock Motatt',
  },],
}

const testdata: SakslisteProps = {
  saker: [{
    sakId: 1, type: 'Mock Barnepensjon', sakstype: 'Mock Nasjonal', behandlinger: [{
      id: 11,
      opprettet: 'Mock 12.01.2021',
      type: 'Mock Revurdering',
      årsak: 'Mock Søknad',
      status: 'Mock Utredes',
      vedtaksdato: 'Mock 18.01.20201',
      resultat: 'Mock Ikke satt',
    }, {
      id: 9,
      opprettet: 'Mock 01.01.2021',
      type: 'Mock Førstegangsbehandling',
      årsak: 'Mock Søknad',
      status: 'Mock Ferdigstilt',
      vedtaksdato: 'Mock 10.01.2021',
      resultat: 'Mock Innvilget',
    },],
  },],
}

export const Person = () => {
  const navigate = useNavigate()
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [personData, setPersonData] = useState({})
  const [personinfo, setPersoninfo] = useState<PersonInfo>()

  const match = useParams<{fnr: string}>()

  const sakIdInput = useRef() as React.MutableRefObject<HTMLInputElement>

  useEffect(() => {
    ;(
      async () => {
        if (match.fnr) {
          const person = await getPerson(match.fnr)
          setPersonData(person)
          setPersoninfo({
            navn: person.data.person.fornavn + person.data.person.etternavn,
            foedselsnummer: person.data.person.foedselsnummer,
            type: 'Etterlatt',
          })
        }
      }
    )()
  }, [])

  const opprettBehandling = () => {
    if (sakIdInput.current.value) {
      opprettBehandlingPaaSak(Number(sakIdInput.current.value))
    }
  }

  const goToBehandling = (behandlingsId: string) => {
    navigate(`/behandling/${behandlingsId}/soeknadsoversikt`)
  }

  return (
    <>
      <StatusBar theme={StatusBarTheme.gray} personInfo={personinfo}/>
      <Container>
        <Tabs>
          <Tlist>
            <TabElement>Saksoversikt</TabElement>
            <TabElement>Dokumentoversikt</TabElement>
          </Tlist>
          <TabPanel>
            <Saksoversikt saksliste={testdata} opprettBehandling={opprettBehandling} goToBehandling={goToBehandling}/>
          </TabPanel>
          <TabPanel>
            <Dokumentoversikt {...testDokumenter} />
          </TabPanel>
        </Tabs>
      </Container>
    </>
  )
}

const TabElement = styled(Tab)`
  margin-left: 10px;
  margin-right: 10px;
`

const Tlist = styled(TabList)`
  display: flex;
  list-style-type: none;
  margin: 1em 0 0;
  li {
    color: var(--nav-blue);
    cursor: pointer;
    padding: 10px 10px 5px;

    &.react-tabs__tab--selected {
      border-top: 1px solid var(--nav-dark-gray);
      border-left: 1px solid var(--nav-dark-gray);
      border-right: 1px solid var(--nav-dark-gray);
      color: var(--nav-dark-gray);
      border-radius: 5px 5px 0 0;
    }
  }
`
