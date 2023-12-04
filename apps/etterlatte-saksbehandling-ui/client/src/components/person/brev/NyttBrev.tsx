import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useParams } from 'react-router-dom'
import { hentBrev } from '~shared/api/brev'
import { useEffect, useState } from 'react'
import { Column, GridContainer } from '~shared/styled'
import { StatusBar } from '~shared/statusbar/Statusbar'
import { getPerson } from '~shared/api/grunnlag'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { BrevStatus } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import styled from 'styled-components'
import NyttBrevHandlingerPanel from '~components/person/brev/NyttBrevHandlingerPanel'
import BrevStatusPanel from '~components/person/brev/BrevStatusPanel'
import NyttBrevMottaker from '~components/person/brev/NyttBrevMottaker'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'

import { mapApiResult } from '~shared/api/apiUtils'

export default function NyttBrev() {
  const { brevId, sakId, fnr } = useParams()
  const [kanRedigeres, setKanRedigeres] = useState(false)

  const [brevStatus, apiHentBrev] = useApiCall(hentBrev)
  const [personStatus, hentPerson] = useApiCall(getPerson)

  useEffect(() => {
    hentPerson(fnr!!)
  }, [])

  useEffect(() => {
    apiHentBrev({ brevId: Number(brevId), sakId: Number(sakId) }, (brev) => {
      if ([BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(brev.status)) {
        setKanRedigeres(true)
      } else {
        setKanRedigeres(false)
      }
    })
  }, [brevId, sakId])

  return (
    <>
      <StatusBar result={personStatus} />
      <NavigerTilbakeMeny label="Tilbake til brevoversikt" path={`/person/${fnr}?fane=BREV`} />

      {mapApiResult(
        brevStatus,
        <Spinner label="Henter brev ..." visible />,
        () => (
          <ApiErrorAlert>Feil oppsto ved henting av brev</ApiErrorAlert>
        ),
        (brev) => (
          <GridContainer>
            <Column>
              <div style={{ margin: '1rem' }}>
                <BrevTittel brevId={brev.id} sakId={brev.sakId} tittel={brev.tittel} />
              </div>
              <NyttBrevMottaker brev={brev} />
            </Column>
            <Column>
              {brev.status === BrevStatus.DISTRIBUERT ? (
                <PanelWrapper>
                  <ForhaandsvisningBrev brev={brev} />
                </PanelWrapper>
              ) : (
                <RedigerbartBrev brev={brev} kanRedigeres={kanRedigeres} />
              )}
            </Column>
            <Column>
              <BrevStatusPanel brev={brev} />
              <NyttBrevHandlingerPanel brev={brev} setKanRedigeres={setKanRedigeres} />
            </Column>
          </GridContainer>
        )
      )}
    </>
  )
}

const PanelWrapper = styled.div`
  height: 100%;
  width: 100%;
  max-height: 955px;
`
