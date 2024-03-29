import { BodyShort, Box, Button, Dropdown, Heading, Label, Modal } from '@navikt/ds-react'
import styled from 'styled-components'
import { ChevronDownIcon, ExternalLinkIcon, EyeIcon } from '@navikt/aksel-icons'
import { useContext, useState } from 'react'
import { OppgavetypeTag, SaktypeTag } from '~components/oppgavebenk/components/Tags'
import { formaterFnr, formaterStringDato } from '~utils/formattering'
import { ConfigContext } from '~clientConfig'
import { FlexRow } from '~shared/styled'
import { FristWrapper } from '~components/oppgavebenk/frist/FristWrapper'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { FerdigstillGosysOppgave } from '../gosys/FerdigstillGosysOppgave'
import { OverfoerOppgaveTilGjenny } from '../gosys/OverfoerOppgaveTilGjenny'

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

export interface GosysActionToggle {
  ferdigstill?: boolean
  konverter?: boolean
}

export const GosysOppgaveModal = ({
  oppgave,
  tilhoererInnloggetSaksbehandler,
}: {
  oppgave: OppgaveDTO
  tilhoererInnloggetSaksbehandler: boolean
}) => {
  const [open, setOpen] = useState(false)
  const [toggle, setToggle] = useState<GosysActionToggle>({})

  const { opprettet, frist, status, fnr, gjelder, enhet, saksbehandler, beskrivelse, sakType, journalpostId } = oppgave

  const configContext = useContext(ConfigContext)

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
                <BodyShort>{journalpostId}</BodyShort>
              </div>
            )}
          </InfoGrid>
          <Box padding="4" borderRadius="medium" borderColor="border-subtle" borderWidth="1" background="bg-subtle">
            <Label>Beskrivelse</Label>
            <BodyShort style={{ whiteSpace: 'pre-wrap' }}>{beskrivelse || <i>Mangler beskrivelse</i>}</BodyShort>
          </Box>

          <br />

          {toggle.ferdigstill ? (
            <FerdigstillGosysOppgave oppgave={oppgave} setToggle={setToggle} />
          ) : toggle.konverter ? (
            <OverfoerOppgaveTilGjenny oppgave={oppgave} setToggle={setToggle} />
          ) : (
            <FlexRow justify="right">
              <Button size="small" variant="tertiary" onClick={() => setOpen(false)}>
                Avbryt
              </Button>

              {tilhoererInnloggetSaksbehandler && (
                <Dropdown>
                  <Button
                    size="small"
                    variant="secondary"
                    icon={<ChevronDownIcon />}
                    iconPosition="right"
                    title="Flere handlinger"
                    as={Dropdown.Toggle}
                  >
                    Flere handlinger
                  </Button>

                  <Dropdown.Menu>
                    <Dropdown.Menu.List>
                      <Dropdown.Menu.List.Item onClick={() => setToggle({ ferdigstill: true })}>
                        Ferdigstill oppgave
                      </Dropdown.Menu.List.Item>

                      {!!journalpostId && (
                        <Dropdown.Menu.List.Item onClick={() => setToggle({ konverter: true })}>
                          Overfør til Gjenny
                        </Dropdown.Menu.List.Item>
                      )}
                    </Dropdown.Menu.List>
                  </Dropdown.Menu>
                </Dropdown>
              )}

              <Button
                size="small"
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
