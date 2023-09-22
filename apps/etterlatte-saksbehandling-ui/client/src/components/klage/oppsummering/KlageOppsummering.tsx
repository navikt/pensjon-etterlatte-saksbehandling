import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import React, { useCallback, useEffect, useRef } from 'react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Innhold } from '~components/klage/styled'
import { useNavigate } from 'react-router-dom'
import { useKlage } from '~components/klage/useKlage'
import Spinner from '~shared/Spinner'
import { kanSeBrev, kanSeOppsummering } from '~components/klage/stegmeny/KlageStegmeny'
import { InnstillingTilKabal, Klage, Omgjoering, TEKSTER_AARSAK_OMGJOERING } from '~shared/types/Klage'
import { JaNei } from '~shared/types/ISvar'
import { isFailure, isInitial, isPending, mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { ferdigstillKlagebehandling } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentBrev } from '~shared/api/brev'
import ForhaandsvisningBrev from '~components/behandling/brev/ForhaandsvisningBrev'
import styled from 'styled-components'

function formaterKlageutfall(klage: Klage) {
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

function VisOmgjoering(props: { omgjoering: Omgjoering }) {
  const { omgjoering } = props

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

      <Alert variant="info">
        Når klagen ferdigstilles vil det opprettes en revurdering for saken, med revurderingsårsak omgjøring etter
        klage.
      </Alert>
    </Maksbredde>
  )
}

function VisInnstilling(props: { innstilling: InnstillingTilKabal; sakId: number }) {
  const { innstilling, sakId } = props
  const ref = useRef<HTMLDialogElement>(null)
  const [brev, hentBrevet] = useApiCall(hentBrev)

  function visModal() {
    if (isInitial(brev) || isFailure(brev)) {
      hentBrevet({ sakId: sakId, brevId: innstilling.brev.brevId })
    }
    ref.current?.showModal()
  }

  return (
    <Maksbredde>
      <Heading size="small" level="3">
        Innstilling til KA
      </Heading>
      <BodyShort spacing>
        Vedtak opprettholdes med følgende hovedhjemmel: <strong>{innstilling.lovhjemmel}.</strong>
      </BodyShort>
      <BodyShort spacing>
        <Button size="small" variant="primary" onClick={visModal}>
          Se innstillingsbrevet
        </Button>
      </BodyShort>

      <Alert variant="info">
        Når klagen ferdigstilles vil innstillingsbrevet bli sendt til mottaker, og klagen blir videresendt for
        behandling av KA.
      </Alert>

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

function VisKlageavslag(props: { klage: Klage }) {
  const { klage } = props
  return <BodyShort>TODO {klage.id} skal få avslagsbrev og håndteres</BodyShort>
}

export function KlageOppsummering() {
  const navigate = useNavigate()
  const klage = useKlage()
  const [ferdigstillStatus, ferdigstill] = useApiCall(ferdigstillKlagebehandling)
  const dispatch = useAppDispatch()

  useEffect(() => {
    if (klage !== null && !kanSeOppsummering(klage)) {
      // Get out
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

        {klage.utfall?.utfall === 'DELVIS_OMGJOERING' || klage.utfall?.utfall === 'STADFESTE_VEDTAK' ? (
          <VisInnstilling innstilling={klage.utfall.innstilling} sakId={klage.sak.id} />
        ) : null}

        {klage.utfall?.utfall === 'DELVIS_OMGJOERING' || klage.utfall?.utfall === 'OMGJOERING' ? (
          <VisOmgjoering omgjoering={klage.utfall.omgjoering} />
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
          onClick={() =>
            kanSeBrev(klage) ? navigate(`/klage/${klage?.id}/brev`) : navigate(`/klage/${klage?.id}/vurdering`)
          }
        >
          Gå tilbake
        </Button>
        <Button variant="primary" onClick={ferdigstillKlage} loading={isPending(ferdigstillStatus)}>
          Ferdigstill klagen
        </Button>
      </FlexRow>
    </Content>
  )
}

const Maksbredde = styled.div`
  max-width: 40rem;
  padding: 1rem 0;
`
