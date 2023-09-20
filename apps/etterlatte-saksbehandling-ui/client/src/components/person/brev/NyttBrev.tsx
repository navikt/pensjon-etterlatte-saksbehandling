import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { isSuccess, useApiCall } from '~shared/hooks/useApiCall'
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

export default function NyttBrev() {
  const { brevId, sakId, fnr } = useParams()
  const [kanRedigeres, setKanRedigeres] = useState(false)

  const [hentetBrev, apiHentBrev] = useApiCall(hentBrev)
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
      <NavigerTilbakeMeny label="Tilbake til brevoversikt" path={`/person/${fnr}/sak/${sakId}/brev`} />

      <GridContainer>
        <Column>{isSuccess(hentetBrev) && <NyttBrevMottaker brev={hentetBrev.data} />}</Column>
        <Column>
          {isSuccess(hentetBrev) &&
            (hentetBrev.data.status === BrevStatus.DISTRIBUERT ? (
              <PanelWrapper>
                <ForhaandsvisningBrev brev={hentetBrev.data} />
              </PanelWrapper>
            ) : (
              <RedigerbartBrev brev={hentetBrev.data} kanRedigeres={kanRedigeres} />
            ))}
        </Column>
        <Column>
          {isSuccess(hentetBrev) && (
            <>
              <BrevStatusPanel brev={hentetBrev.data} />
              <NyttBrevHandlingerPanel brev={hentetBrev.data} setKanRedigeres={setKanRedigeres} />
            </>
          )}
        </Column>
      </GridContainer>
    </>
  )
}

const PanelWrapper = styled.div`
  height: 100%;
  width: 100%;
  max-height: 955px;
`
