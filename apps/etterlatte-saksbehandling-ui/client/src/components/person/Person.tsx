import { useEffect } from 'react'
import { Tab, TabList, TabPanel, Tabs } from 'react-tabs'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { Container } from '~shared/styled'
import { Dokumentoversikt } from './dokumentoversikt'
import { Saksoversikt } from './saksoversikt'
import Spinner from '~shared/Spinner'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { BodyShort } from '@navikt/ds-react'
import { getPerson } from '~shared/api/grunnlag'
import { GYLDIG_FNR } from '~utils/fnr'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'

export const Person = () => {
  const [personStatus, hentPerson] = useApiCall(getPerson)

  const { fnr } = useParams()

  useEffect(() => {
    if (GYLDIG_FNR(fnr)) {
      hentPerson(fnr!!)
    }
  }, [fnr])

  if (isFailure(personStatus)) {
    return (
      <Container>
        <BodyShort>{!GYLDIG_FNR(fnr) ? 'Fødselsnummeret i URLen er ugyldig' : JSON.stringify(personStatus)}</BodyShort>
      </Container>
    )
  }

  return (
    <>
      <StatusBar result={personStatus} />
      <NavigerTilbakeMeny label={'Tilbake til oppgavebenken'} path={'/'} />

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
