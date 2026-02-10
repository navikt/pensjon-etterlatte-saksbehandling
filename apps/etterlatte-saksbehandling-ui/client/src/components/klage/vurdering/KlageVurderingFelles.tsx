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
import { Alert, BodyShort, Box, Button, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import ForhaandsvisningBrev, { PdfViewer } from '~components/behandling/brev/ForhaandsvisningBrev'
import styled from 'styled-components'
import { useKlage } from '~components/klage/useKlage'
import { forhaandsvisBlankettKa } from '~shared/api/klage'
import { EnvelopeClosedIcon, EyeIcon } from '@navikt/aksel-icons'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { TekstMedMellomrom } from '~shared/TekstMedMellomrom'

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
      <VStack gap="space-6">
        <Box>
          <Heading size="small" level="3">
            Innstilling til KA
          </Heading>
          <BodyShort>
            Vedtak opprettholdes med følgende hovedhjemmel:{' '}
            <strong>{TEKSTER_LOVHJEMLER[innstilling.lovhjemmel]}.</strong>
          </BodyShort>
        </Box>
        <Box>
          <Heading size="xsmall" level="4">
            Innstillingstekst:
          </Heading>
          <TekstMedMellomrom>{innstilling.innstillingTekst}</TekstMedMellomrom>
        </Box>
        <Box>
          <Heading size="xsmall" level="4">
            Intern kommentar:
          </Heading>
          <TekstMedMellomrom>{innstilling.internKommentar || 'Ikke registrert'}</TekstMedMellomrom>
        </Box>
        <HStack gap="space-2">
          <Button icon={<EyeIcon />} size="small" variant="secondary" onClick={visOversendelseModal}>
            Oversendelsesbrev til klager
          </Button>
          <Button icon={<EyeIcon />} size="small" variant="secondary" onClick={visBlankettModal}>
            Innstillingsbrev til KA
          </Button>
        </HStack>

        {kanRedigere && (
          <Alert variant="info">
            Når klagen ferdigstilles vil oversendelsesbrevet bli sendt til klager, og klagen blir videresendt for
            behandling i klageinstans.
          </Alert>
        )}

        <Modal width="medium" ref={oversendelseRef} header={{ heading: 'Oversendelsesbrev til klager' }}>
          <Modal.Body>
            {mapResult(oversendelseBrev, {
              pending: <Spinner label="Laster brevet" />,
              success: (hentetBrev) => <ForhaandsvisningBrev brev={hentetBrev} />,
              error: <ApiErrorAlert>Kunne ikke hente brevet, prøv å laste siden på nytt.</ApiErrorAlert>,
            })}
          </Modal.Body>
        </Modal>

        <Modal width="medium" ref={blankettRef} header={{ heading: 'Innstillingsbrev til KA' }}>
          <Modal.Body>
            {mapResult(forhaandsvisningBlankett, {
              pending: <Spinner label="Henter pdf" />,
              success: () => (blankettFileUrl ? <PdfViewer src={`${blankettFileUrl}#toolbar=0`} /> : null),
              error: <ApiErrorAlert>Kunne ikke forhåndsvise blanketten. Prøv å laste siden på nytt</ApiErrorAlert>,
            })}
          </Modal.Body>
        </Modal>
      </VStack>
    </Maksbredde>
  )
}

export function VisOmgjoering(props: { omgjoering: Omgjoering; kanRedigere: boolean }) {
  const { omgjoering, kanRedigere } = props

  return (
    <Maksbredde>
      <VStack gap="space-6">
        <Box>
          <Heading size="small" level="3">
            Omgjøring
          </Heading>
          <BodyShort>
            Vedtaket omgjøres på grunn av <strong>{TEKSTER_AARSAK_OMGJOERING[omgjoering.grunnForOmgjoering]}.</strong>
          </BodyShort>
        </Box>
        <Box>
          <Heading size="xsmall" level="4">
            Begrunnelse:
          </Heading>
          <TekstMedMellomrom>{omgjoering.begrunnelse}</TekstMedMellomrom>
        </Box>
        {kanRedigere && (
          <Alert variant="info">Når klagen ferdigstilles vil det opprettes en oppgave for omgjøring av vedtaket.</Alert>
        )}
      </VStack>
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

export const ButtonNavigerTilBrev = (props: { klage: Klage }) => {
  return (
    <PersonButtonLink
      fnr={props.klage.sak.ident}
      fane={PersonOversiktFane.BREV}
      variant="primary"
      icon={<EnvelopeClosedIcon aria-hidden />}
      target="_blank"
      rel="noreferrer noopener"
    >
      Opprett brev
    </PersonButtonLink>
  )
}
