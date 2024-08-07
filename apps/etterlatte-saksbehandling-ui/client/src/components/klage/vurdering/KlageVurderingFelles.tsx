import {
  InnstillingTilKabal,
  Klage,
  Omgjoering,
  TEKSTER_AARSAK_OMGJOERING,
  TEKSTER_LOVHJEMLER,
} from '~shared/types/Klage'
import React, { useRef, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrev } from '~shared/api/brev'
import { isFailure, isInitial, mapResult } from '~shared/api/apiUtils'
import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import ForhaandsvisningBrev, { PdfViewer } from '~components/behandling/brev/ForhaandsvisningBrev'
import styled from 'styled-components'
import { useKlage } from '~components/klage/useKlage'
import { forhaandsvisBlankettKa } from '~shared/api/klage'
import { EnvelopeClosedIcon } from '@navikt/aksel-icons'
import { NavLink } from 'react-router-dom'

export function VisInnstilling(props: { innstilling: InnstillingTilKabal; sakId: number; kanRedigere: boolean }) {
  const klage = useKlage()

  const { innstilling, sakId, kanRedigere } = props
  const oversendelseRef = useRef<HTMLDialogElement>(null)
  const blankettRef = useRef<HTMLDialogElement>(null)
  const [oversendelseBrev, hentOversendelseBrev] = useApiCall(hentBrev)
  const [forhaandsvisningBlankett, hentForhaandsvisning] = useApiCall(forhaandsvisBlankettKa)
  const [blankettFileUrl, setBlankettFileUrl] = useState<string>()

  function visBlankettModal() {
    if (!innstilling || !klage) return
    if (isInitial(forhaandsvisningBlankett) || isFailure(forhaandsvisningBlankett)) {
      hentForhaandsvisning({ klage }, (bytes) => {
        const blob = new Blob([bytes], { type: 'application/pdf' })
        const fileUrl = URL.createObjectURL(blob)

        setBlankettFileUrl(fileUrl)
        setTimeout(() => {
          URL.revokeObjectURL(fileUrl)
        }, 1000)
      })
    }
    blankettRef.current?.showModal()
  }

  function visOversendelseModal() {
    if (!innstilling) return

    if (isInitial(oversendelseBrev) || isFailure(oversendelseBrev)) {
      hentOversendelseBrev({ sakId: sakId, brevId: innstilling.brev.brevId })
    }
    oversendelseRef.current?.showModal()
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
      <Heading size="xsmall" level="4">
        Innstillingstekst:
      </Heading>
      <BodyShort spacing>{innstilling.innstillingTekst}</BodyShort>
      <Heading size="xsmall" level="4">
        Intern kommentar:
      </Heading>
      <BodyShort spacing>{innstilling.internKommentar || 'Ikke registrert'}</BodyShort>

      <BodyShort spacing>
        <Button size="small" variant="primary" onClick={visOversendelseModal}>
          Se innstillingsbrevet
        </Button>
      </BodyShort>

      <BodyShort spacing>
        <Button size="small" variant="primary" onClick={visBlankettModal}>
          Se blankett til KA
        </Button>
      </BodyShort>

      {kanRedigere && (
        <Alert variant="info">
          Når klagen ferdigstilles vil innstillingsbrevet bli sendt til mottaker, og klagen blir videresendt for
          behandling av KA.
        </Alert>
      )}

      <Modal width="medium" ref={oversendelseRef} header={{ heading: 'Innstillingsbrev' }}>
        <Modal.Body>
          {mapResult(oversendelseBrev, {
            pending: <Spinner visible label="Laster brevet" />,
            success: (hentetBrev) => <ForhaandsvisningBrev brev={hentetBrev} />,
            error: <ApiErrorAlert>Kunne ikke hente brevet, prøv å laste siden på nytt.</ApiErrorAlert>,
          })}
        </Modal.Body>
      </Modal>

      <Modal width="medium" ref={blankettRef} header={{ heading: 'Blankett til KA' }}>
        <Modal.Body>
          {mapResult(forhaandsvisningBlankett, {
            pending: <Spinner visible label="Henter pdf" />,
            success: () => (blankettFileUrl ? <PdfViewer src={`${blankettFileUrl}#toolbar=0`} /> : null),
            error: <ApiErrorAlert>Kunne ikke forhåndsvise blanketten. Prøv å laste siden på nytt</ApiErrorAlert>,
          })}
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

export function formaterKlageutfall(klage: Klage) {
  switch (klage.utfall?.utfall) {
    case 'DELVIS_OMGJOERING':
      return 'delvis omgjøring'
    case 'STADFESTE_VEDTAK':
      return 'stadfesting av vedtak'
    case 'OMGJOERING':
      return 'omgjøring av vedtak'
    case 'AVVIST':
      return 'avvist med vedtak'
    case 'AVVIST_MED_OMGJOERING':
      return 'avvist med omgjøring av vedtak'
  }
  return 'ukjent'
}

const Maksbredde = styled.div`
  max-width: 40rem;
  padding: 1rem 0;
`
/**
 * TODO: Denne blir det også krøll med ifm. history state
 **/
export const ButtonNavigerTilBrev = (props: { klage: Klage }) => {
  return (
    <Button
      as={NavLink}
      variant="primary"
      icon={<EnvelopeClosedIcon />}
      to="/person?fane=BREV"
      state={props.klage.sak.ident}
      target="_blank"
    >
      Opprett brev
    </Button>
  )
}
