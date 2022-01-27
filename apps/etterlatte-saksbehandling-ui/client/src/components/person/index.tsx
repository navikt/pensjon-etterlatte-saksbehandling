import { useEffect, useRef, useState } from 'react'
import { Tab, Tabs, TabList, TabPanel } from 'react-tabs'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { getPerson, opprettBehandlingPaaSak, opprettSakPaaPerson } from '../../shared/api/person'
import { StatusBar, StatusBarTheme } from '../statusbar'
import { Container } from '../../shared/styled'
import { Saksliste, SakslisteProps } from './saksliste'

const testdata: SakslisteProps = {
    saker: [
      {
        verdi: [
          {
            col: 'Opprettet',
            value: '12.01.2021',
          },
          {
            col: 'Type',
            value: 'Barnepensjon',
          },
          {
            col: 'Status',
            value: 'Opprettet',
          },
          {
            col: 'Vedtaksdato',
            value: '18.01.2021',
          },
          {
            col: 'Resultat',
            value: 'Vedtatt',
          },
        ],
      },
      {
        verdi: [
          {
            col: 'Opprettet',
            value: '12.01.2021',
          },
          {
            col: 'Type',
            value: 'Barnepensjon',
          },
          {
            col: 'Status',
            value: 'Opprettet',
          },
          {
            col: 'Vedtaksdato',
            value: '18.01.2021',
          },
          {
            col: 'Resultat',
            value: 'Vedtatt',
          },
        ],
      },
      {
        verdi: [
          {
            col: 'Opprettet',
            value: '12.01.2021',
          },
          {
            col: 'Type',
            value: 'Barnepensjon',
          },
          {
            col: 'Status',
            value: 'Opprettet',
          },
          {
            col: 'Vedtaksdato',
            value: '18.01.2021',
          },
          {
            col: 'Resultat',
            value: 'Vedtatt',
          },
        ],
      },
    ],
}

export const Person = () => {
  const [personData, setPersonData] = useState({})
  const match = useParams<{ fnr: string }>()

  const sakIdInput = useRef() as React.MutableRefObject<HTMLInputElement>

  console.log(personData)
  useEffect(() => {
    ;(async () => {
      if (match.fnr) {
        const person = await getPerson(match.fnr)
        setPersonData(person)
      }
    })()
  }, [])

  const opprettSak = () => {
    if (match.fnr) {
      opprettSakPaaPerson(match.fnr)
    }
  }

  const opprettBehandling = () => {
    if (sakIdInput.current.value) {
      console.log(sakIdInput.current.value)
      opprettBehandlingPaaSak(Number(sakIdInput.current.value))
    }
  }

  return (
    <>
      <StatusBar theme={StatusBarTheme.gray} />

      <Container>
        <Tabs>
          <Tlist>
            <Tab>Personopplysninger</Tab>
            <Tab>Behandlingsoversikt</Tab>
            <Tab>Stønadshisstorikk</Tab>
            <Tab>Modia meldinger</Tab>
            <Tab>Dokumentoversikt</Tab>
            <Tab>Testgreier</Tab>
          </Tlist>

          <TabPanel>
            <Saksliste {...testdata} />
          </TabPanel>
          <TabPanel>
            <h2>Any content 2</h2>
          </TabPanel>
          <TabPanel>
            <h2>Any content 3</h2>
          </TabPanel>
          <TabPanel>
            <h2>Any content 4</h2>
          </TabPanel>
          <TabPanel>
            <h2>Any content 5</h2>
          </TabPanel>
          <TabPanel>
            <p>
              <button onClick={opprettSak}>Opprett/hent sak</button>
            </p>
            <p>
              <input ref={sakIdInput} placeholder="sakid" name="sakid" />
              <button onClick={opprettBehandling}>Opprett behandling på denne saken</button>
            </p>
          </TabPanel>
        </Tabs>
      </Container>
    </>
  )
}

const Tlist = styled(TabList)`
  display: flex;
  list-style-type: none;
  margin: 1em 0 0;
  justify-content: space-between;
  li {
    color: var(--nav-blue);
    cursor: pointer;
  }
`
