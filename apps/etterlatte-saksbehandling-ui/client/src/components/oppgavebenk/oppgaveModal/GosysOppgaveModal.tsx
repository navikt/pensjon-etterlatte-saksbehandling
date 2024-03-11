import { Alert, BodyShort, Box, Button, Heading, Label, Loader, Modal } from '@navikt/ds-react'
import styled from 'styled-components'
import { CheckmarkIcon, ExternalLinkIcon, EyeIcon } from '@navikt/aksel-icons'
import { useContext, useState } from 'react'
import { OppgavetypeTag, SaktypeTag } from '~components/oppgavebenk/components/Tags'
import { formaterFnr, formaterStringDato } from '~utils/formattering'
import { ferdigstilleGosysOppgave, OppgaveDTO } from '~shared/api/oppgaver'
import { ConfigContext } from '~clientConfig'
import { FlexRow } from '~shared/styled'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'

const TagRow = styled.div`
  display: flex;
  gap: 1rem;
  margin: 1.5rem 0;
`

const InfoGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
`

export const GosysOppgaveModal = ({
  oppgave,
  tilhoererInnloggetSaksbehandler,
}: {
  oppgave: OppgaveDTO
  tilhoererInnloggetSaksbehandler: boolean
}) => {
  const [open, setOpen] = useState(false)
  const [toggleFerdigstill, setToggleFerdigstill] = useState(false)

  const { opprettet, frist, status, fnr, gjelder, enhet, saksbehandler, beskrivelse, sakType, journalpostId } = oppgave

  const [ferdigstillResult, ferdigstillOppgave] = useApiCall(ferdigstilleGosysOppgave)

  const configContext = useContext(ConfigContext)

  const ferdigstill = () =>
    ferdigstillOppgave({ oppgaveId: oppgave.id, versjon: oppgave.versjon || 0 }, () => {
      setTimeout(() => window.location.reload(), 2000)
    })

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon />} onClick={() => setOpen(true)}>
        Se oppgave
      </Button>
      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Header>
          <Heading size="medium" id="modal-heading">
            {journalpostId ? 'Journalføringsoppgave fra Gosys' : 'Oppgave fra Gosys'}
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <TagRow>
            <SaktypeTag sakType={sakType} />
            <OppgavetypeTag oppgavetype="GOSYS" />
          </TagRow>
          <InfoGrid>
            <div>
              <Label>Reg.dato</Label>
              <BodyShort>{formaterStringDato(opprettet)}</BodyShort>
            </div>
            <div>
              <Label>Frist</Label>
              <BodyShort>
                <FristWrapper dato={frist} />
              </BodyShort>
            </div>
            <div>
              <Label>Status</Label>
              <BodyShort>{status}</BodyShort>
            </div>
            <div>
              <Label>Fødselsnummer</Label>
              <BodyShort>{fnr ? formaterFnr(fnr) : <i>Mangler</i>}</BodyShort>
            </div>
            <div>
              <Label>Gjelder</Label>
              <BodyShort>{gjelder}</BodyShort>
            </div>
            <div>
              <Label>Enhet</Label>
              <BodyShort>{enhet}</BodyShort>
            </div>
            <div>
              <Label>Saksbehandler</Label>
              <BodyShort>{saksbehandler?.ident || '-'}</BodyShort>
            </div>
            {!!journalpostId && (
              <div>
                <Label>JournalpostId</Label>
                <BodyShort>{journalpostId || '-'}</BodyShort>
              </div>
            )}
          </InfoGrid>
          <Box padding="4" borderRadius="medium" borderColor="border-subtle" borderWidth="1" background="bg-subtle">
            <Label>Beskrivelse</Label>
            <BodyShort style={{ whiteSpace: 'pre-wrap' }}>{beskrivelse || <i>Mangler beskrivelse</i>}</BodyShort>
          </Box>

          <br />

          {mapFailure(ferdigstillResult, (error) => (
            <Alert variant="error">{error.detail || 'Ukjent feil oppsto ved ferdigstilling av oppgave'}</Alert>
          ))}

          {toggleFerdigstill ? (
            isSuccess(ferdigstillResult) ? (
              <Alert variant="success">
                Oppgaven ble ferdigstilt. Henter oppgaver på nytt <Loader />
              </Alert>
            ) : (
              <>
                <Alert variant="info">Er du sikker på at du vil ferdigstille oppgaven?</Alert>

                <br />

                <FlexRow justify="right">
                  <Button variant="secondary" onClick={() => setToggleFerdigstill(false)}>
                    Nei, avbryt
                  </Button>
                  <Button onClick={ferdigstill} loading={isPending(ferdigstillResult)}>
                    Ja, ferdigstill
                  </Button>
                </FlexRow>
              </>
            )
          ) : (
            <FlexRow justify="right">
              <Button variant="tertiary" onClick={() => setOpen(false)}>
                Avbryt
              </Button>
              {tilhoererInnloggetSaksbehandler && (
                <Button onClick={() => setToggleFerdigstill(true)} variant="secondary" icon={<CheckmarkIcon />}>
                  Ferdigstill oppgave
                </Button>
              )}
              <Button
                variant="primary"
                as="a"
                href={fnr ? `${configContext['gosysUrl']}/personoversikt/fnr=${fnr}` : configContext['gosysUrl']}
                target="_blank"
                icon={<ExternalLinkIcon />}
              >
                Åpne i Gosys
              </Button>
            </FlexRow>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
