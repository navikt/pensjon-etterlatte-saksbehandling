import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Alert, BodyShort, Button, HStack, Modal, Textarea, VStack } from '@navikt/ds-react'
import { ArrowUndoIcon } from '@navikt/aksel-icons'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { BrevStatus, IBrev } from '~shared/types/Brev'
import { differenceInWeeks } from 'date-fns'
import { journalfoerBrev, markerBrevSomUtgaar } from '~shared/api/brev'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'

const EN_UKE = 1

/**
 * Sjekker om brevet er ferdigstilt eller journalført og over 1 uke gammelt.
 * Dette er som oftest et tegn på at noe har gått galt i flyten
 **/
const kanMarkereSomUtgaar = (brev: IBrev) =>
  [BrevStatus.FERDIGSTILT, BrevStatus.JOURNALFOERT].includes(brev.status) &&
  differenceInWeeks(new Date(), brev.statusEndret) >= EN_UKE

/**
 * Handling som kan brukes i tilfeller hvor et brev ikke lenger er relevant/aktuelt.
 **/
export const BrevUtgaar = ({ brev }: { brev: IBrev }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [kommentar, setKommentar] = useState<string>()
  const [brevUtgaarResult, apiBrevUtgaar] = useApiCall(markerBrevSomUtgaar)
  const [journalfoerResult, apiJournalfoerBrev] = useApiCall(journalfoerBrev)

  if (!kanMarkereSomUtgaar(brev)) {
    return null
  }

  const utgaar = () => {
    if (!kommentar) return

    apiBrevUtgaar({ brevId: brev.id, sakId: brev.sakId, kommentar }, () => window.location.reload())
  }

  const journalfoer = () => apiJournalfoerBrev({ brevId: brev.id, sakId: brev.sakId })

  return (
    <>
      <Button
        variant="danger"
        icon={<ArrowUndoIcon aria-hidden />}
        onClick={() => setIsOpen(true)}
        title="Marker som utgår"
        size="small"
      >
        Utgår
      </Button>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-label="Slett brev" header={{ heading: 'Brev utgår' }}>
        <Modal.Body>
          <VStack gap="space-4">
            <BodyShort spacing>
              Brevet er mer enn {EN_UKE} uke gammelt og ikke distribuert. Vurder om brevet skal utgå.
            </BodyShort>

            <BodyShort spacing>
              Vær obs på at markering av brev med status <strong>UTGÅR</strong> ikke kan angres!
            </BodyShort>

            {brev.status === BrevStatus.FERDIGSTILT &&
              mapResult(journalfoerResult, {
                initial: (
                  <Alert variant="warning">
                    <HStack gap="space-4" align="center">
                      <BodyShort>Brevet er ikke journalført. Vil du journalføre brevet?</BodyShort>
                      <Button size="small" variant="secondary" onClick={journalfoer}>
                        Ja, journalfør
                      </Button>
                    </HStack>
                  </Alert>
                ),
                pending: <Spinner label="Journalfører brevet" />,
                error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
                success: () => <Alert variant="success">Journalført OK</Alert>,
              })}

            {brev.status === BrevStatus.JOURNALFOERT && (
              <Alert variant="warning">
                OBS: Brevet har status journalført. Hvis journalposten skal avbrytes/utgå må det gjøres manuelt via
                dokumentoversikten.
              </Alert>
            )}

            <Textarea
              label="Kommentar"
              name="Kommentar"
              description="Skriv hvorfor brevet utgår eller annen relevant informasjon"
              value={kommentar || ''}
              onChange={(e) => setKommentar(e.target.value)}
            />

            {mapResult(brevUtgaarResult, {
              error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
              success: () => <Alert variant="success">Brev markert som utgått. Laster siden på nytt...</Alert>,
            })}

            <HStack gap="space-4" justify="center">
              <Button variant="secondary" onClick={() => setIsOpen(false)} disabled={isPending(brevUtgaarResult)}>
                Nei, avbryt
              </Button>
              <Button variant="danger" onClick={utgaar} loading={isPending(brevUtgaarResult)} disabled={!kommentar}>
                Ja, brevet utgår
              </Button>
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
