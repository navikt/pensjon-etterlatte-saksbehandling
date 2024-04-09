import { Alert, BodyShort, Button, Modal, Textarea } from '@navikt/ds-react'
import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import InstitusjonsoppholdVurderingBegrunnelse from '~components/person/uhaandtereHendelser/InstitusjonsoppholdVurderingBegrunnelse'
import { ButtonGroup } from '~components/person/VurderHendelseModal'
import { isPending, mapSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lukkGrunnlagshendelse } from '~shared/api/behandling'
import { XMarkIcon } from '@navikt/aksel-icons'
import { hentOppgaveForReferanseUnderBehandling } from '~shared/api/oppgaver'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const LukkHendelseModal = ({ hendelse }: { hendelse: Grunnlagsendringshendelse }) => {
  const [open, setOpen] = useState(false)
  const [kommentar, setKommentar] = useState<string>('')
  const [lukkHendelseResult, lukkGrunnlagshendelseFunc, resetApiCall] = useApiCall(lukkGrunnlagshendelse)
  const [oppgaveResult, hentOppgave] = useApiCall(hentOppgaveForReferanseUnderBehandling)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const aapneModal = () => {
    hentOppgave(hendelse.id)
    setOpen(true)
  }

  const lukkGrunnlagshendelseWrapper = () => {
    lukkGrunnlagshendelseFunc(
      { ...hendelse, kommentar },
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
      <Button variant="tertiary" onClick={aapneModal} icon={<XMarkIcon />} style={{ float: 'right' }}>
        Lukk hendelse
      </Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        aria-labelledby="modal-heading"
        style={{ maxWidth: '60rem' }}
        header={{ heading: 'Lukk hendelse' }}
      >
        <Modal.Body>
          {hendelse.type === GrunnlagsendringsType.INSTITUSJONSOPPHOLD ? (
            <>
              <InstitusjonsoppholdVurderingBegrunnelse
                sakId={hendelse.sakId}
                grunnlagsEndringshendelseId={hendelse.id}
                lukkGrunnlagshendelseWrapper={lukkGrunnlagshendelseWrapper}
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
              <Textarea label="Begrunnelse" value={kommentar} onChange={(e) => setKommentar(e.target.value)} />

              {isFailureHandler({
                apiResult: lukkHendelseResult,
                errorMessage: 'Vi kunne ikke lukke hendelsen',
              })}

              <br />

              {mapSuccess(oppgaveResult, (oppgave) => {
                const tildeltIdent = oppgave?.saksbehandler?.ident

                if (!tildeltIdent) {
                  return (
                    <Alert variant="info">
                      Oppgaven er ikke tildelt en saksbehandler. Om du lukker hendelsen vil den automatisk tildeles deg.
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
                    setKommentar('')
                    resetApiCall()
                    setOpen(false)
                  }}
                >
                  Avbryt
                </Button>
                <Button
                  onClick={lukkGrunnlagshendelseWrapper}
                  disabled={!kommentar}
                  loading={isPending(lukkHendelseResult)}
                >
                  Lagre
                </Button>
              </ButtonGroup>
            </>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
