import {
  InnstillingTilKabal,
  Klage,
  Omgjoering,
  TEKSTER_AARSAK_OMGJOERING,
  TEKSTER_LOVHJEMLER,
} from '~shared/types/Klage'
import React, { useRef } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import { isFailure, isInitial, mapApiResult } from '~shared/api/apiUtils'
import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import styled from 'styled-components'
import { JaNei } from '~shared/types/ISvar'

export function VisInnstilling(props: { innstilling: InnstillingTilKabal; sakId: number; kanRedigere: boolean }) {
  const { innstilling, sakId, kanRedigere } = props
  const ref = useRef<HTMLDialogElement>(null)
  const [brev, hentBrevet] = useApiCall(hentBrev)

  function visModal() {
    if (!innstilling) return

    if (isInitial(brev) || isFailure(brev)) {
      hentBrevet({ sakId: sakId, brevId: innstilling.brev.brevId })
    }
    ref.current?.showModal()
  }

  if (!innstilling) return null

  return (
    <Maksbredde>
      <Heading size="small" level="3">
        Innstilling til KA
      </Heading>
      <BodyShort spacing>
        Vedtak opprettholdes med følgende hovedhjemmel: <strong>{TEKSTER_LOVHJEMLER[innstilling.lovhjemmel]}.</strong>
      </BodyShort>
      <BodyShort spacing>
        <Button size="small" variant="primary" onClick={visModal}>
          Se innstillingsbrevet
        </Button>
      </BodyShort>

      {kanRedigere && (
        <Alert variant="info">
          Når klagen ferdigstilles vil innstillingsbrevet bli sendt til mottaker, og klagen blir videresendt for
          behandling av KA.
        </Alert>
      )}

      <Modal width="medium" ref={ref} header={{ heading: 'Innstillingsbrev' }}>
        <Modal.Body>
          {mapApiResult(
            brev,
            <Spinner visible label="Laster brevet" />,
            () => (
              <ApiErrorAlert>Kunne ikke hente brevet, prøv å laste siden på nytt.</ApiErrorAlert>
            ),
            (hentetBrev) => (
              <ForhaandsvisningBrev brev={hentetBrev} />
            )
          )}
        </Modal.Body>
      </Modal>
    </Maksbredde>
  )
}

export function VisOmgjoering(props: { omgjoering: Omgjoering; kanRedigere: boolean }) {
  const { omgjoering, kanRedigere } = props

  return (
    <Maksbredde>
      <Heading size="small" level="3">
        Omgjøring
      </Heading>
      <BodyShort spacing>
        Vedtaket omgjøres på grunn av <strong>{TEKSTER_AARSAK_OMGJOERING[omgjoering.grunnForOmgjoering]}.</strong>
      </BodyShort>
      <BodyShort>
        <strong>Begrunnelse:</strong>
      </BodyShort>
      <BodyShort spacing>{omgjoering.begrunnelse}</BodyShort>

      {kanRedigere && (
        <Alert variant="info">Når klagen ferdigstilles vil det opprettes en oppgave for omgjøring av vedtaket.</Alert>
      )}
    </Maksbredde>
  )
}

export function VisKlageavslag(props: { klage: Klage }) {
  const { klage } = props
  return <BodyShort>TODO {klage.id} skal få avslagsbrev og håndteres</BodyShort>
}

export function formaterKlageutfall(klage: Klage) {
  if (klage.formkrav?.formkrav.erFormkraveneOppfylt === JaNei.NEI) {
    return 'avvist på grunn av formkrav'
  }
  switch (klage.utfall?.utfall) {
    case 'DELVIS_OMGJOERING':
      return 'delvis omgjøring'
    case 'STADFESTE_VEDTAK':
      return 'stadfesting av vedtak'
    case 'OMGJOERING':
      return 'omgjøring av vedtak'
  }
  return 'ukjent'
}

const Maksbredde = styled.div`
  max-width: 40rem;
  padding: 1rem 0;
`
