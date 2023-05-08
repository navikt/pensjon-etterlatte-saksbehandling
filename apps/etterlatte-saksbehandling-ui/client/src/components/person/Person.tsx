import { useEffect } from 'react'
import { Tab, TabList, TabPanel, Tabs } from 'react-tabs'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { StatusBar, StatusBarTheme } from '~shared/statusbar/Statusbar'
import { Container } from '~shared/styled'
import { Dokumentoversikt } from './dokumentoversikt'
import { Saksoversikt } from './saksoversikt'
import Spinner from '~shared/Spinner'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { BodyShort, Label } from '@navikt/ds-react'
import { getPerson } from '~shared/api/grunnlag'
import { INVALID_FNR } from '~utils/fnr'

export const Person = () => {
  const [personStatus, hentPerson] = useApiCall(getPerson)

  const match = useParams<{ fnr: string }>()

  useEffect(() => {
    if (match.fnr) {
      hentPerson(match.fnr)
    }
  }, [match.fnr])

  const personInfo = isSuccess(personStatus)
    ? {
        fornavn: personStatus.data.fornavn,
        mellomnavn: personStatus.data.mellomnavn,
        etternavn: personStatus.data.etternavn,
        fnr: personStatus.data.foedselsnummer,
      }
    : null

  if (isFailure(personStatus)) {
    return (
      <Container>
        <BodyShort>
          {INVALID_FNR(match.fnr) ? 'Fødselsnummeret i URLen er ugyldig' : JSON.stringify(personStatus)}
        </BodyShort>
      </Container>
    )
  }

  return (
    <>
      {match.fnr === null && <Label>Kan ikke hente fødselsnummer fra URL</Label>}
      {personInfo && <StatusBar theme={StatusBarTheme.gray} personInfo={personInfo} />}
      {isPending(personStatus) && <Spinner visible={true} label={'Laster'} />}
      {isSuccess(personStatus) && (
        <Container>
          <Tabs>
            <Tlist>
              <TabElement>Saksoversikt</TabElement>
              <TabElement>Dokumentoversikt</TabElement>
            </Tlist>
            <TabPanel>
              <Saksoversikt fnr={personStatus.data.foedselsnummer} />
            </TabPanel>
            <TabPanel>
              <Dokumentoversikt fnr={personStatus.data.foedselsnummer} />
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
