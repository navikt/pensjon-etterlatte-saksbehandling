import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isInitial, isPending, isPendingOrInitial, isSuccess, mapApiResult, mapSuccess } from '~shared/api/apiUtils'
import { opprettOmgjoeringKlage } from '~shared/api/revurdering'
import { hentKlage } from '~shared/api/klage'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Klage, Omgjoering, VedtaketKlagenGjelder } from '~shared/types/Klage'
import { formaterVedtakType } from '~utils/formatering/formatering'
import { formaterKanskjeStringDato } from '~utils/formatering/dato'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/types/oppgave'

function hentOmgjoering(klage: Klage): Omgjoering | null {
  if (klage.kabalResultat === 'MEDHOLD') {
    return {
      begrunnelse: 'Klagen har fått medhold i behandlingen i Klageinstansen',
      grunnForOmgjoering: 'ANNET',
    }
  }

  if (klage.utfall?.utfall === 'DELVIS_OMGJOERING' || klage.utfall?.utfall === 'OMGJOERING') {
    return klage.utfall.omgjoering
  }
  return null
}

function hentVedtakKlagesPaa(klage: Klage): VedtaketKlagenGjelder | null {
  return klage.formkrav?.formkrav?.vedtaketKlagenGjelder ?? null
}

function erBehandlingVedtakOmgjoering(klage: Klage): boolean {
  const omgjoering = hentOmgjoering(klage)
  if (omgjoering === null) {
    return false
  }

  const vedtakKlagesPaa = hentVedtakKlagesPaa(klage)
  switch (vedtakKlagesPaa?.vedtakType) {
    case 'INNVILGELSE':
    case 'ENDRING':
    case 'OPPHOER':
      return true
    case 'AVSLAG':
      return false
    default:
      return false
  }
}

export function OmgjoerVedtakModal({ oppgave }: { oppgave: OppgaveDTO }) {
  const [open, setOpen] = useState(false)
  const [opprettRevurderingStatus, opprettRevurdering] = useApiCall(opprettOmgjoeringKlage)
  const [klageResult, fetchKlage] = useApiCall(hentKlage)

  if (!erOppgaveRedigerbar(oppgave?.status)) return null

  useEffect(() => {
    if (oppgave.referanse) {
      fetchKlage(oppgave.referanse)
    }
  }, [oppgave.referanse])

  const klage = mapSuccess(klageResult, (hentetKlage) => hentetKlage)

  function opprett() {
    if (!klage) {
      // Skal ikke være mulig med disabled
      return
    }

    if (erBehandlingVedtakOmgjoering(klage) && isInitial(opprettRevurderingStatus)) {
      opprettRevurdering({
        oppgaveId: oppgave.id,
        sakId: oppgave.sakId,
      })
    }
  }

  return (
    <>
      <Button variant="primary" size="small" onClick={() => setOpen(true)} style={{ textAlign: 'left' }}>
        Omgjør vedtak
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Header>
          <Heading size="medium" id="modal-heading">
            Omgjør vedtak
          </Heading>
        </Modal.Header>
        <Modal.Body>
          {mapApiResult(
            klageResult,
            <Spinner label="Henter klage" />,
            (error) => (
              <ApiErrorAlert>{error.detail}</ApiErrorAlert>
            ),
            (klage) => {
              const vedtak = hentVedtakKlagesPaa(klage)
              if (vedtak === null) {
                return (
                  <Alert variant="warning">
                    Klagen er ikke koblet til et vedtak som skal omgjøres. Dette skal ikke skje, meld sak i porten med
                    saksnummer
                  </Alert>
                )
              }
              return (
                <>
                  <BodyShort>
                    Vedtaket om {formaterVedtakType(vedtak.vedtakType!!)} attestert{' '}
                    {formaterKanskjeStringDato(vedtak.datoAttestert)} skal omgjøres.
                  </BodyShort>
                  {!erBehandlingVedtakOmgjoering(klage) && (
                    <Alert variant="warning">
                      Det er ikke støttet å omgjøre vedtak som ikke er behandlinger enda.{' '}
                    </Alert>
                  )}
                </>
              )
            }
          )}
          {mapSuccess(opprettRevurderingStatus, (behandling) => (
            <>
              <BodyShort spacing>Revurdering for omgjøring av vedtak er opprettet. </BodyShort>

              <Button variant="primary" as="a" href={`/behandling/${behandling.id}`}>
                Åpne revurdering
              </Button>
            </>
          ))}
          {isFailureHandler({
            apiResult: opprettRevurderingStatus,
            errorMessage:
              'Kunne ikke opprette revurdering for omgjøring. Prøv på nytt senere, og meld sak hvis problemet vedvarer',
          })}
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="primary"
            onClick={opprett}
            loading={isPending(opprettRevurderingStatus)}
            disabled={isSuccess(opprettRevurderingStatus) || isPendingOrInitial(klageResult)}
          >
            Opprett revurdering
          </Button>
          <Button variant="tertiary" onClick={() => setOpen(false)} disabled={isPending(opprettRevurderingStatus)}>
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
