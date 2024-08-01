import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useParams } from 'react-router-dom'
import { hentBrev } from '~shared/api/brev'
import { useEffect, useState } from 'react'
import { Column, GridContainer } from '~shared/styled'
import { StatusBarPersonHenter } from '~shared/statusbar/Statusbar'
import NavigerTilbakeMeny from '~components/person/NavigerTilbakeMeny'
import { BrevProsessType, BrevStatus } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import styled from 'styled-components'
import NyttBrevHandlingerPanel from '~components/person/brev/NyttBrevHandlingerPanel'
import BrevStatusPanel from '~components/person/brev/BrevStatusPanel'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'

import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import { BrevMottaker } from '~components/person/brev/mottaker/BrevMottaker'
import { Box, Heading } from '@navikt/ds-react'
import BrevSpraak from '~components/person/brev/spraak/BrevSpraak'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { hentSak } from '~shared/api/sak'
import { useAppDispatch } from '~store/Store'
import { settSak } from '~store/reducers/SakReducer'

export default function NyttBrev() {
  useSidetittel('Nytt brev')

  const dispatch = useAppDispatch()

  const [fnr, setFnr] = useState<string>()
  const { sakId, brevId } = useParams()
  const [sakResult, sakFetch] = useApiCall(hentSak)
  const [kanRedigeres, setKanRedigeres] = useState(false)
  const [brevStatus, apiHentBrev] = useApiCall(hentBrev)

  useEffect(() => {
    sakFetch(Number(sakId))
  }, [sakId])

  useEffect(() => {
    if (isSuccess(sakResult) && sakResult.data) {
      dispatch(settSak(sakResult.data))
      setFnr(sakResult.data.ident)
      apiHentBrev({ brevId: Number(brevId), sakId: sakResult.data.id }, (brev) => {
        if ([BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(brev.status)) {
          setKanRedigeres(true)
        } else {
          setKanRedigeres(false)
        }
      })
    } else if (sakResult.status == 'error') {
      throw new Error('Kunne ikke hente sak med sak ID ' + sakId)
    }
  }, [brevId, sakResult, fnr])

  return (
    <>
      {fnr && <StatusBarPersonHenter ident={fnr || ''} />}
      <NavigerTilbakeMeny label="Tilbake til brevoversikt" path={`/person/${sakId}?fane=BREV`} />

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
                <BrevTittel brevId={brev.id} sakId={brev.sakId} tittel={brev.tittel} kanRedigeres={kanRedigeres} />
              </div>
              <div style={{ margin: '1rem' }}>
                <BrevSpraak brev={brev} kanRedigeres={kanRedigeres} />
              </div>
              <div style={{ margin: '1rem' }}>
                <BrevMottaker brev={brev} kanRedigeres={kanRedigeres} />
              </div>
            </Column>
            <Column>
              {brev.prosessType === BrevProsessType.OPPLASTET_PDF || brev.status === BrevStatus.DISTRIBUERT ? (
                <PanelWrapper>
                  <ForhaandsvisningBrev brev={brev} />
                </PanelWrapper>
              ) : (
                <RedigerbartBrev brev={brev} kanRedigeres={kanRedigeres} />
              )}
            </Column>
            <Column>
              <BrevStatusPanel brev={brev} />
              <Box padding="4" borderRadius="small">
                <Heading spacing level="2" size="medium">
                  Handlinger
                </Heading>
                <NyttBrevHandlingerPanel brev={brev} setKanRedigeres={setKanRedigeres} />
              </Box>
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
