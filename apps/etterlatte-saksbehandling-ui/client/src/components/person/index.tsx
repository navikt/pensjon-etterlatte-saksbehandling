import { useEffect, useState } from 'react'
import { Tab, TabList, TabPanel, Tabs } from 'react-tabs'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { getPerson } from '../../shared/api/person'
import { StatusBar, StatusBarTheme } from '../../shared/statusbar'
import { Container } from '../../shared/styled'
import { Dokumentoversikt } from './dokumentoversikt'
import { Saksoversikt } from './saksoversikt'
import { Dokumenter, IPersonResult } from './typer'
import Spinner from '../../shared/Spinner'

const testDokumenter: Dokumenter = {
  brev: [
    {
      dato: 'Mock 13.05.2021',
      tittel: 'Mock Innvilgelsesbrev barnepensjon',
      link: 'link',
      status: 'Mock Sendt ut',
    },
    {
      dato: 'Mock 09.05.2021',
      tittel: 'Mock Søknad barnepensjon - førstegangsbehandling',
      link: 'link',
      status: 'Mock Motatt',
    },
  ],
}

export const Person = () => {
  const [personData, setPersonData] = useState<IPersonResult | undefined>(undefined)
  const [lastet, setLastet] = useState<boolean>(false)
  const [error, setError] = useState<IPersonResult>()

  const match = useParams<{ fnr: string }>()

  useEffect(() => {
    const getPersonAsync = async (fnr: string) => {
      const response = await getPerson(fnr)

      if (response.status === 'ok') {
        setPersonData(response?.data)
      } else {
        setError(response?.error)
      }

      setLastet(true)
    }

    if (match.fnr) {
      getPersonAsync(match.fnr)
    }
  }, [])

  if (error && personData === undefined) {
    return (
      <Container>
        <div>{JSON.stringify(error)}</div>
      </Container>
    )
  }

  const navn = personData?.person.fornavn + ' ' + personData?.person.etternavn
  const personInfo = personData ? { navn: navn, fnr: personData?.person.foedselsnummer, type: 'Etterlatt' } : null

  return (
    <>
      {personInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={personInfo} />}
      <Spinner visible={!lastet} label={'Laster'} />
      {lastet && (
        <Container>
          <Tabs>
            <Tlist>
              <TabElement>Saksoversikt</TabElement>
              <TabElement>Dokumentoversikt</TabElement>
            </Tlist>
            <TabPanel>
              <Saksoversikt
                behandlingliste={personData?.behandlingListe.behandlinger}
                grunnlagshendelser={personData?.grunnlagsendringshendelser?.hendelser}
              />
            </TabPanel>
            <TabPanel>
              <Dokumentoversikt {...testDokumenter} fnr={match.fnr} />
            </TabPanel>
          </Tabs>
        </Container>
      )}
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
