/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import { Alert, BodyShort, Box, Button, Dropdown, Heading, HStack, Label, Modal } from '@navikt/ds-react'
import styled from 'styled-components'
import { ChevronDownIcon, ExternalLinkIcon, EyeIcon } from '@navikt/aksel-icons'
import React, { useContext, useState } from 'react'
import { formaterDato } from '~utils/formatering/dato'
import { ConfigContext } from '~clientConfig'
import { FerdigstillGosysOppgave } from '../gosys/FerdigstillGosysOppgave'
import { OverfoerOppgaveTilGjenny } from '../gosys/OverfoerOppgaveTilGjenny'
import { formaterOppgavetype, GosysOppgave } from '~shared/types/Gosys'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { GosysBrukerWrapper } from '~components/oppgavebenk/gosys/GosysBrukerWrapper'
import { GosysTemaTag } from '~shared/tags/GosysTemaTag'
import { StatusPaaOppgaveFrist } from '~components/oppgavebenk/frist/StatusPaaOppgaveFrist'

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

export const GosysOppgaveModal = ({ oppgave }: { oppgave: GosysOppgave }) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [open, setOpen] = useState(false)
  const [toggle, setToggle] = useState<GosysActionToggle>({})

  const { opprettet, frist, status, bruker, enhet, saksbehandler, beskrivelse, tema, oppgavetype, journalpostId } =
    oppgave

  const configContext = useContext(ConfigContext)

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon aria-hidden />} onClick={() => setOpen(true)}>
        Se oppgave
      </Button>

      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Header>
          <Heading size="medium" id="modal-heading">
            {journalpostId ? 'Journalføringsoppgave fra Gosys' : 'Oppgave fra Gosys'}
          </Heading>
        </Modal.Header>

        <Modal.Body>
          {innloggetSaksbehandler.ident !== oppgave.saksbehandler?.ident && (
            <Alert variant="warning">Du kan kun endre denne oppgaven hvis du er tildelt den.</Alert>
          )}

          <TagRow>
            <GosysTemaTag tema={tema} />
          </TagRow>
          <InfoGrid>
            <div>
              <Label>Reg.dato</Label>
              <BodyShort>{formaterDato(opprettet)}</BodyShort>
            </div>
            <div>
              <Label>Frist</Label>
              {frist ? (
                <StatusPaaOppgaveFrist oppgaveFrist={frist} oppgaveStatus={oppgave.status} />
              ) : (
                <BodyShort>Ingen frist</BodyShort>
              )}
            </div>
            <div>
              <Label>Status</Label>
              <BodyShort>{status}</BodyShort>
            </div>
            <div>
              <Label>Oppgavetype</Label>
              <BodyShort>{formaterOppgavetype(oppgavetype)}</BodyShort>
            </div>
            <div>
              <Label>Fødselsnummer</Label>
              <BodyShort as="div">
                <GosysBrukerWrapper bruker={bruker} />
              </BodyShort>
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
          <Box padding="space-4" borderWidth="1" borderColor="border-neutral-subtle">
            <Label>Beskrivelse</Label>
            <BodyShort style={{ whiteSpace: 'pre-wrap' }}>{beskrivelse || <i>Mangler beskrivelse</i>}</BodyShort>
          </Box>

          <br />

          {toggle.ferdigstill ? (
            <FerdigstillGosysOppgave oppgave={oppgave} setToggle={setToggle} />
          ) : toggle.konverter ? (
            <OverfoerOppgaveTilGjenny oppgave={oppgave} setToggle={setToggle} />
          ) : (
            <HStack gap="space-4" justify="end">
              <Button size="small" variant="tertiary" onClick={() => setOpen(false)}>
                Avbryt
              </Button>

              {innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident && (
                <Dropdown>
                  <Button
                    size="small"
                    variant="secondary"
                    icon={<ChevronDownIcon aria-hidden />}
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

                      {/* Vi støtter foreløpig KUN flytting av journalføringsoppgaver */}
                      {!!journalpostId && oppgavetype.includes('JFR') && (
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
                href={
                  bruker?.ident
                    ? `${configContext['gosysUrl']}/personoversikt/fnr=${bruker?.ident}`
                    : configContext['gosysUrl']
                }
                target="_blank"
                icon={<ExternalLinkIcon aria-hidden />}
              >
                Åpne i Gosys
              </Button>
            </HStack>
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
