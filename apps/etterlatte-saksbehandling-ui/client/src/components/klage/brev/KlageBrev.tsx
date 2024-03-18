import { useKlage } from '~components/klage/useKlage'
import { useNavigate } from 'react-router-dom'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { BodyShort, Button, Heading } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { Klage } from '~shared/types/Klage'
import { useApiCall } from '~shared/hooks/useApiCall'
import { BrevProsessType, BrevStatus, kanBrevRedigeres } from '~shared/types/Brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import RedigerbartBrev from '~components/behandling/brev/RedigerbartBrev'
import { hentBrev } from '~shared/api/brev'
import styled from 'styled-components'
import { ApiErrorAlert } from '~ErrorBoundary'
import { JaNei } from '~shared/types/ISvar'
import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import BrevTittel from '~components/person/brev/tittel/BrevTittel'
import { forrigeSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { BrevMottaker } from '~components/person/brev/mottaker/BrevMottaker'

function hentBrevId(klage: Klage | null): number | null {
  switch (klage?.utfall?.utfall) {
    case 'DELVIS_OMGJOERING':
    case 'STADFESTE_VEDTAK':
      return klage.utfall.innstilling.brev.brevId
    case 'AVVIST':
      return klage.utfall.brev.brevId
    default:
      return null
  }
}

export function KlageBrev() {
  const navigate = useNavigate()
  const klage = useKlage()

  const brevId = hentBrevId(klage)
  const sakId = klage?.sak?.id
  const [hentetBrev, apiHentBrev] = useApiCall(hentBrev)

  useEffect(() => {
    if (!sakId || !brevId) {
      return
    }
    void apiHentBrev({ brevId, sakId })
  }, [brevId, sakId])

  if (!klage) {
    return <Spinner visible label="Henter klage" />
  }

  return (
    <Content>
      <BrevContent>
        <Sidebar>
          <ContentHeader>
            <HeadingWrapper>
              <Heading spacing level="1" size="large">
                Brev
              </Heading>
            </HeadingWrapper>
            <BodyShort spacing>
              {klage.formkrav?.formkrav.erKlagenFramsattInnenFrist === JaNei.JA
                ? 'Oversendelsesbrev til klager'
                : 'Avvisningsbrev til klager'}
            </BodyShort>
            {isSuccess(hentetBrev) && (
              <>
                <BrevTittel
                  brevId={hentetBrev.data.id}
                  sakId={hentetBrev.data.sakId}
                  tittel={hentetBrev.data.tittel}
                  kanRedigeres={true}
                />
                <br />
                <BrevMottaker brev={hentetBrev.data} kanRedigeres={true} />
              </>
            )}
          </ContentHeader>
        </Sidebar>

        {mapApiResult(
          hentetBrev,
          <SpinnerContainer>
            <Spinner visible label="Henter brevet" />
          </SpinnerContainer>,
          () => (
            <ApiErrorAlert>Kunne ikke hente brevet. Prøv å laste siden på nytt</ApiErrorAlert>
          ),
          (brev) => {
            if (brev.status === BrevStatus.DISTRIBUERT || brev.prosessType !== BrevProsessType.REDIGERBAR) {
              return <ForhaandsvisningBrev brev={brev} />
            } else {
              return <RedigerbartBrev brev={brev} kanRedigeres={kanBrevRedigeres(brev.status)} />
            }
          }
        )}
      </BrevContent>

      <Border />

      <div>
        <FlexRow justify="center" $spacing>
          <Button className="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'brev'))}>
            Gå tilbake
          </Button>
          <Button className="button" variant="primary" onClick={() => navigate(`/klage/${klage.id}/oppsummering`)}>
            Se oppsummering
          </Button>
        </FlexRow>
      </div>
    </Content>
  )
}

const BrevContent = styled.div`
  display: flex;
`

const SpinnerContainer = styled.div`
  height: 100%;
  width: 100%;
  text-align: center;
`

const Sidebar = styled.div`
  max-height: fit-content;
  min-width: 40%;
  width: 40%;
  border-right: 1px solid #c6c2bf;
`
