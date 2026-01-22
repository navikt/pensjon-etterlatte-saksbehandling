import { Alert, BodyShort, Button, Heading, Modal } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending, isPendingOrInitial, isSuccess, mapApiResult, mapResult, mapSuccess } from '~shared/api/apiUtils'
import { opprettOmgjoeringFoerstegangsbehandling, opprettOmgjoeringKlage } from '~shared/api/revurdering'
import { hentKlage } from '~shared/api/klage'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { harOmgjoering, Klage, Omgjoering, VedtaketKlagenGjelder } from '~shared/types/Klage'
import { formaterVedtakType } from '~utils/formatering/formatering'
import { formaterKanskjeStringDato } from '~utils/formatering/dato'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/types/oppgave'
import { VedtakType } from '~components/vedtak/typer'

function hentOmgjoering(klage: Klage): Omgjoering | null {
  if (klage.kabalResultat === 'MEDHOLD') {
    return {
      begrunnelse: 'Klagen har fått medhold i behandlingen i Klageinstansen',
      grunnForOmgjoering: 'ANNET',
    }
  }

  if (harOmgjoering(klage.utfall)) {
    return klage.utfall.omgjoering
  }
  return null
}

function hentVedtakKlagesPaa(klage: Klage): VedtaketKlagenGjelder | null {
  return klage.formkrav?.formkrav?.vedtaketKlagenGjelder ?? null
}

function finnOmgjoeringsHandlingForKlage(klage: Klage): OmgjoerHandling {
  const omgjoering = hentOmgjoering(klage)
  if (omgjoering === null) {
    return OmgjoerHandling.IKKE_STOETTET
  }

  const vedtakKlagesPaa = hentVedtakKlagesPaa(klage)
  switch (vedtakKlagesPaa?.vedtakType) {
    case VedtakType.INNVILGELSE:
    case VedtakType.ENDRING:
    case VedtakType.OPPHOER:
      return OmgjoerHandling.REVURDERING
    case VedtakType.AVSLAG:
      return OmgjoerHandling.OMGJOERING_FOERSTEGANGSBEHANDLING
    default:
      return OmgjoerHandling.IKKE_STOETTET
  }
}

enum OmgjoerHandling {
  REVURDERING = 'REVURDERING',
  OMGJOERING_FOERSTEGANGSBEHANDLING = 'OMGJOERING_FOERSTEGANGSBEHANDLING',
  IKKE_STOETTET = 'IKKE_STOETTET',
}

export function OmgjoerVedtakModal({ oppgave }: { oppgave: OppgaveDTO }) {
  const [open, setOpen] = useState(false)
  const [disabledOpprett, setDisabledOpprett] = useState(false)
  const [opprettRevurderingStatus, opprettRevurdering] = useApiCall(opprettOmgjoeringKlage)
  const [opprettOmgjoeringFoerstegangsbehandlingStatus, opprettOmgjoeringFoerstegangsbehandlingApi] = useApiCall(
    opprettOmgjoeringFoerstegangsbehandling
  )
  const [klageResult, fetchKlage] = useApiCall(hentKlage)

  if (!erOppgaveRedigerbar(oppgave?.status)) return null

  const klage = mapSuccess(klageResult, (hentetKlage) => hentetKlage)

  useEffect(() => {
    if (oppgave.referanse) {
      fetchKlage(oppgave.referanse)
    }
  }, [oppgave.referanse])

  useEffect(() => {
    if (klage) {
      setDisabledOpprett(finnOmgjoeringsHandlingForKlage(klage) == OmgjoerHandling.IKKE_STOETTET)
    }
  }, [klage])

  function opprett() {
    if (!klage) {
      // Skal ikke være mulig med disabled
      return
    }

    const handling = finnOmgjoeringsHandlingForKlage(klage)

    if (handling === OmgjoerHandling.REVURDERING) {
      opprettRevurdering({
        oppgaveId: oppgave.id,
        sakId: oppgave.sakId,
      })
    } else if (handling === OmgjoerHandling.OMGJOERING_FOERSTEGANGSBEHANDLING) {
      opprettOmgjoeringFoerstegangsbehandlingApi({
        sakId: oppgave.sakId,
        omgjoeringRequest: {
          skalKopiere: true,
          erSluttbehandlingUtland: false,
          omgjoeringsOppgaveId: oppgave.id,
        },
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
                  {!finnOmgjoeringsHandlingForKlage(klage) ||
                    (disabledOpprett && (
                      <Alert variant="warning">
                        Det er ikke støttet å omgjøre vedtak som ikke er behandlinger enda.
                      </Alert>
                    ))}
                </>
              )
            }
          )}

          {mapResult(opprettRevurderingStatus, {
            success: (behandling) => (
              <>
                <BodyShort spacing>Revurdering for omgjøring av vedtak er opprettet. </BodyShort>

                <Button variant="primary" as="a" href={`/behandling/${behandling.id}`}>
                  Åpne revurdering
                </Button>
              </>
            ),
            error: (error) => (
              <ApiErrorAlert>
                Kunne ikke opprette revurdering for omgjøring. Prøv på nytt senere, og meld sak hvis problemet vedvarer:{' '}
                {error.detail}
              </ApiErrorAlert>
            ),
          })}

          {mapResult(opprettOmgjoeringFoerstegangsbehandlingStatus, {
            success: (behandling) => (
              <>
                <BodyShort spacing>Omgjøring av førstegangsbehandling er opprettet. </BodyShort>

                <Button variant="primary" as="a" href={`/behandling/${behandling.id}`}>
                  Åpne behandling
                </Button>
              </>
            ),
            error: (error) => (
              <ApiErrorAlert>
                Kunne ikke opprette omgjøring førstegangsbehandling på grunn av feil: {error.detail}
              </ApiErrorAlert>
            ),
          })}
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="primary"
            onClick={opprett}
            loading={isPending(opprettRevurderingStatus) || isPending(opprettOmgjoeringFoerstegangsbehandlingStatus)}
            disabled={
              isSuccess(opprettRevurderingStatus) ||
              isSuccess(opprettOmgjoeringFoerstegangsbehandlingStatus) ||
              isPendingOrInitial(klageResult) ||
              disabledOpprett
            }
          >
            Opprett revurdering
          </Button>
          <Button
            variant="tertiary"
            onClick={() => setOpen(false)}
            disabled={isPending(opprettRevurderingStatus) || isPending(opprettOmgjoeringFoerstegangsbehandlingStatus)}
          >
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
}
