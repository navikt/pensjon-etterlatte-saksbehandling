import { Alert, BodyShort, Button, Modal, Textarea } from '@navikt/ds-react'
import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import InstitusjonsoppholdVurderingBegrunnelse from '~components/person/hendelser/InstitusjonsoppholdVurderingBegrunnelse'
import { isPending, mapSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lukkGrunnlagshendelse } from '~shared/api/behandling'
import { ArchiveIcon } from '@navikt/aksel-icons'
import { hentOppgaveForReferanseUnderBehandling } from '~shared/api/oppgaver'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ButtonGroup } from '~shared/styled'

export const ArkiverHendelseModal = ({ hendelse }: { hendelse: Grunnlagsendringshendelse }) => {
  const [open, setOpen] = useState(false)
  const [begrunnelse, setBegrunnelse] = useState<string>('')
  const [arkiverHendelseResult, arkiverHendelseFunc, resetApiCall] = useApiCall(lukkGrunnlagshendelse)
  const [oppgaveResult, hentOppgave] = useApiCall(hentOppgaveForReferanseUnderBehandling)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const aapneModal = () => {
    hentOppgave(hendelse.id)
    setOpen(true)
  }

  const arkiverHendelse = () => {
    arkiverHendelseFunc(
      { ...hendelse, kommentar: begrunnelse },
      () => {
        setOpen(false)
        location.reload()
      },
      (err) => {
        console.error(`Feil status: ${err.status} error: ${err.detail}`)
      }
    )
  }

  return (
    <>
      <Button variant="tertiary" onClick={aapneModal} icon={<ArchiveIcon />} size="small">
        Arkiver hendelse
      </Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        aria-labelledby="modal-heading"
        header={{ heading: 'Arkiver hendelse' }}
      >
        <Modal.Body>
          {hendelse.type === GrunnlagsendringsType.INSTITUSJONSOPPHOLD ? (
            <>
              <InstitusjonsoppholdVurderingBegrunnelse
                sakId={hendelse.sakId}
                grunnlagsEndringshendelseId={hendelse.id}
                lukkGrunnlagshendelseWrapper={arkiverHendelse}
              />
              <Button variant="secondary" onClick={() => setOpen(false)}>
                Avbryt
              </Button>
            </>
          ) : (
            <>
              <BodyShort spacing>
                I noen tilfeller krever ikke ny informasjon eller hendelser noen revurdering. Beskriv hvorfor en
                revurdering ikke er nødvendig.
              </BodyShort>
              <Textarea label="Begrunnelse" value={begrunnelse} onChange={(e) => setBegrunnelse(e.target.value)} />

              {isFailureHandler({
                apiResult: arkiverHendelseResult,
                errorMessage: 'Vi kunne ikke arkivere hendelsen',
              })}

              <br />

              {mapSuccess(oppgaveResult, (oppgave) => {
                const tildeltIdent = oppgave?.saksbehandler?.ident

                if (!tildeltIdent) {
                  return (
                    <Alert variant="info" inline>
                      Oppgaven er ikke tildelt en saksbehandler. Om du arkiverer hendelsen vil den automatisk tildeles
                      deg.
                    </Alert>
                  )
                } else if (tildeltIdent !== innloggetSaksbehandler.ident) {
                  return <Alert variant="warning">Oppgaven tilhører {oppgave?.saksbehandler?.navn}</Alert>
                }
              })}

              <ButtonGroup>
                <Button
                  variant="secondary"
                  onClick={() => {
                    setBegrunnelse('')
                    resetApiCall()
                    setOpen(false)
                  }}
                >
                  Avbryt
                </Button>
                <Button onClick={arkiverHendelse} disabled={!begrunnelse} loading={isPending(arkiverHendelseResult)}>
                  Arkiver
                </Button>
              </ButtonGroup>
            </>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
