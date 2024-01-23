import { BodyShort, Button, Heading } from '@navikt/ds-react'
import React, { useCallback, useEffect } from 'react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Innhold } from '~components/klage/styled'
import { useNavigate } from 'react-router-dom'
import { useKlage } from '~components/klage/useKlage'
import Spinner from '~shared/Spinner'
import { forrigeSteg, kanSeOppsummering } from '~components/klage/stegmeny/KlageStegmeny'
import { JaNei } from '~shared/types/ISvar'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillKlagebehandling } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { ApiErrorAlert } from '~ErrorBoundary'

import { isFailure, isPending } from '~shared/api/apiUtils'
import {
  formaterKlageutfall,
  VisInnstilling,
  VisKlageavslag,
  VisOmgjoering,
} from '~components/klage/vurdering/KlageVurderingFelles'

export function KlageOppsummering({ kanRedigere }: { kanRedigere: boolean }) {
  const navigate = useNavigate()
  const klage = useKlage()
  const [ferdigstillStatus, ferdigstill] = useApiCall(ferdigstillKlagebehandling)
  const dispatch = useAppDispatch()

  useEffect(() => {
    if (klage !== null && !kanSeOppsummering(klage)) {
      navigate(`/klage/${klage.id}/`)
    }
  }, [klage])

  const ferdigstillKlage = useCallback(() => {
    ferdigstill(klage!!.id, (ferdigKlage) => {
      dispatch(addKlage(ferdigKlage))
      // Kanskje gi noe eksplisitt feedback? Evt håndtere oppsummering av en ferdig klage forskjellig.
      // Gjelder forsåvidt de andre stegene i behandlingen også, visning for utfylt skjema
    })
  }, [klage?.id])

  if (!klage) {
    return <Spinner visible label="Henter klage" />
  }

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Oppsummering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

      <Innhold>
        <Heading size="medium" level="2">
          Utfall
        </Heading>
        <BodyShort spacing>Utfallet av klagen er {formaterKlageutfall(klage)}.</BodyShort>

        {klage.formkrav?.formkrav.erFormkraveneOppfylt === JaNei.NEI ? <VisKlageavslag klage={klage} /> : null}

        <VisInnstilling klage={klage} kanRedigere={kanRedigere} />

        {klage.utfall?.utfall === 'DELVIS_OMGJOERING' || klage.utfall?.utfall === 'OMGJOERING' ? (
          <VisOmgjoering klage={klage} kanRedigere={kanRedigere} />
        ) : null}
      </Innhold>

      {isFailure(ferdigstillStatus) ? (
        <ApiErrorAlert>
          Kunne ikke ferdigstille klagebehandling på grunn av en feil. Prøv igjen etter å ha lastet siden på nytt, og
          meld sak hvis problemet vedvarer.
        </ApiErrorAlert>
      ) : null}

      <FlexRow justify="center" $spacing>
        <Button
          variant="secondary"
          onClick={() => navigate(`/klage/${klage?.id}/${forrigeSteg(klage, 'oppsummering')}`)}
        >
          Gå tilbake
        </Button>
        {kanRedigere && (
          <Button variant="primary" onClick={ferdigstillKlage} loading={isPending(ferdigstillStatus)}>
            Ferdigstill klagen
          </Button>
        )}
      </FlexRow>
    </Content>
  )
}
