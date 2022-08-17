import { useEffect, useState } from 'react'
import { Tab, Tabs, TabList, TabPanel } from 'react-tabs'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { getPerson } from '../../shared/api/person'
import { StatusBar, StatusBarTheme } from '../statusbar'
import { Container } from '../../shared/styled'
import { Dokumentoversikt } from './dokumentoversikt'
import { Saksoversikt } from './saksoversikt'
import { Dokumenter, IPersonResult } from './typer'
import { IApiResponse } from "../../shared/api/types";

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

export const Person = () => {
  const [personData, setPersonData] = useState<IPersonResult | undefined>(undefined)
  const [loaded, setLoaded] = useState<boolean>(false)

  const match = useParams<{fnr: string}>()

  useEffect(() => {
    if (match.fnr) {
      getPerson(match.fnr).then((result: IApiResponse<IPersonResult>) => {
        setPersonData(result?.data)
        setLoaded(true)
      })
    }
  }, [])

  const navn = personData?.person.fornavn + ' ' + personData?.person.etternavn
  const personInfo = personData ? {navn: navn, fnr: personData?.person.foedselsnummer, type: 'Etterlatt'} : null

  return (
    <>
      {personInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={personInfo}/>}
      {loaded && (
        <Container>
          <Tabs>
            <Tlist>
              <TabElement>Saksoversikt</TabElement>
              <TabElement>Dokumentoversikt</TabElement>
            </Tlist>
            <TabPanel>
              <Saksoversikt behandlingliste={personData?.behandlingListe.behandlinger}/>
            </TabPanel>
            <TabPanel>
              <Dokumentoversikt {...testDokumenter} />
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
