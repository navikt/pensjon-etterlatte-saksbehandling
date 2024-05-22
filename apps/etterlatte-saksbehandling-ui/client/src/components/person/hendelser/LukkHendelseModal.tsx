import { Alert, BodyShort, Button, Modal, Textarea } from '@navikt/ds-react'
import { Grunnlagsendringshendelse, GrunnlagsendringsType } from '~components/person/typer'
import { isPending, mapSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lukkGrunnlagshendelse } from '~shared/api/behandling'
import { ArchiveIcon } from '@navikt/aksel-icons'
import { hentOppgaveForReferanseUnderBehandling } from '~shared/api/oppgaver'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { ButtonGroup } from '~shared/styled'
import { VurderInstitusjonsoppholdModalBody } from '~components/person/hendelser/institusjonsopphold/VurderInstitusjonsoppholdModalBody'

export const LukkHendelseModal = ({ hendelse }: { hendelse: Grunnlagsendringshendelse }) => {
  const [open, setOpen] = useState(false)
  const [kommentar, setKommentar] = useState<string>('')
  const [lukkHendelseResult, lukkHendelseFunc, resetApiCall] = useApiCall(lukkGrunnlagshendelse)
  const [oppgaveResult, hentOppgave] = useApiCall(hentOppgaveForReferanseUnderBehandling)

  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const aapneModal = () => {
    hentOppgave(hendelse.id)
    setOpen(true)
  }

  const lukkHendelse = () => {
    lukkHendelseFunc(
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
      <Button variant="tertiary" onClick={aapneModal} icon={<ArchiveIcon />} size="small">
        Lukk hendelse
      </Button>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        aria-labelledby="modal-heading"
        header={{ heading: 'Lukk hendelse' }}
      >
        {hendelse.type === GrunnlagsendringsType.INSTITUSJONSOPPHOLD ? (
          <VurderInstitusjonsoppholdModalBody
            setOpen={setOpen}
            sakId={hendelse.sakId}
            hendelseId={hendelse.id}
            lukkHendelse={lukkHendelse}
          />
        ) : (
          <Modal.Body>
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
                  <Alert variant="info" inline>
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
              <Button onClick={lukkHendelse} disabled={!kommentar} loading={isPending(lukkHendelseResult)}>
                Lukk
              </Button>
            </ButtonGroup>
          </Modal.Body>
        )}
      </Modal>
    </>
  )
}
