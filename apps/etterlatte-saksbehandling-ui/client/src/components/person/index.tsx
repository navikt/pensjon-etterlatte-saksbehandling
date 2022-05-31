import { useEffect, useRef, useState } from 'react'
import { Tab, Tabs, TabList, TabPanel } from 'react-tabs'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { getPerson, opprettBehandlingPaaSak, opprettSakPaaPerson } from '../../shared/api/person'
import { PersonInfo, StatusBar, StatusBarTheme } from '../statusbar'
import { Container } from '../../shared/styled'
import { SakslisteProps } from './saksliste'
import { Dokumentoversikt } from './dokumentoversikt'
import { Saksoversikt } from './saksoversikt'

//todo: typer
const testDokumenter = {
  dokumenter: [
    {
      kolonner: [
        {
          col: 'Dato',
          value: '13.05.2021',
        },
        {
          col: 'Tittel',
          value: 'Innvilgelsesbrev barnepensjon',
          link: 'link',
        },

        {
          col: 'Status',
          value: 'Sendt ut',
        },
      ],
    },
    {
      kolonner: [
        {
          col: 'Dato',
          value: '09.05.2021',
        },
        {
          col: 'Tittel',
          value: 'Søknad barnepensjon - førstegangsbehandling',
          link: 'link',
        },

        {
          col: 'Status',
          value: 'Motatt',
        },
      ],
    },
  ],
}

const testdata: SakslisteProps = {
  saker: [
    {
      behandlinger: [
        {
          kolonner: [
            {
              col: 'Opprettet',
              value: '12.01.2021',
            },
            {
              col: 'Type',
              value: 'Revurdering',
            },
            {
              col: 'Årsak',
              value: 'Søknad',
            },
            {
              col: 'Status',
              value: 'Utredes',
            },
            {
              col: 'Vedtaksdato',
              value: '18.01.2021',
            },
            {
              col: 'Resultat',
              value: 'Ikke satt',
            },
          ],
        },
        {
          kolonner: [
            {
              col: 'Opprettet',
              value: '12.01.2021',
            },
            {
              col: 'Type',
              value: 'Førstegangsbehandling',
            },
            {
              col: 'årsak',
              value: 'Søknad',
            },
            {
              col: 'Status',
              value: 'Ferdigstilt',
            },
            {
              col: 'Vedtaksdato',
              value: '18.01.2021',
            },
            {
              col: 'Resultat',
              value: 'Innvilget',
            },
          ],
        },
      ],
    },
  ],
}

export const Person = () => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [personData, setPersonData] = useState({})
  const [personinfo, setPersoninfo] = useState<PersonInfo>()

  const match = useParams<{ fnr: string }>()

  const sakIdInput = useRef() as React.MutableRefObject<HTMLInputElement>

  useEffect(() => {
    ;(async () => {
      if (match.fnr) {
        const person = await getPerson(match.fnr)
        setPersonData(person)
        setPersoninfo({
          fornavn: person.data.person.fornavn,
          etternavn: person.data.person.etternavn,
          foedselsnummer: person.data.person.foedselsnummer,
          type: 'Etterlatt',
        })
      }
    })()
  }, [])
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const opprettSak = () => {
    if (match.fnr) {
      opprettSakPaaPerson(match.fnr)
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const opprettBehandling = () => {
    if (sakIdInput.current.value) {
      opprettBehandlingPaaSak(Number(sakIdInput.current.value))
    }
  }

  return (
    <>
      <StatusBar theme={StatusBarTheme.gray} personInfo={personinfo} />
      <Container>
        <Tabs>
          <Tlist>
            <TabElement>Saksoversikt</TabElement>
            <TabElement>Dokumentoversikt</TabElement>
          </Tlist>

          <TabPanel>
            <Saksoversikt {...testdata} />
          </TabPanel>
          <TabPanel>
            <Dokumentoversikt {...testDokumenter} />
          </TabPanel>

          {/** <TabPanel>
            <p>
              <button onClick={opprettSak}>Opprett/hent sak</button>
            </p>
            <p>
              <input ref={sakIdInput} placeholder="sakid" name="sakid" />
              <button onClick={opprettBehandling}>Opprett behandling på denne saken</button>
            </p>
          </TabPanel>
          */}
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
