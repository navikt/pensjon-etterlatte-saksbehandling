import { BodyShort, Button, Heading, Label, Modal } from '@navikt/ds-react'
import styled from 'styled-components'
import { EyeIcon } from '@navikt/aksel-icons'
import { useContext, useState } from 'react'
import { OppgavetypeTag, SaktypeTag } from '~components/oppgavebenk/Tags'
import { formaterFnr, formaterStringDato } from '~utils/formattering'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { ConfigContext } from '~clientConfig'
import { FlexRow } from '~shared/styled'
import { FristWrapper } from '~components/oppgavebenk/FristWrapper'

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

const BeskrivelseWrapper = styled.div`
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  width: 46rem;
`

export const GosysOppgaveModal = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const [open, setOpen] = useState(false)
  const { opprettet, frist, status, fnr, gjelder, enhet, saksbehandler, beskrivelse, sakType } = oppgave

  const configContext = useContext(ConfigContext)

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon />} onClick={() => setOpen(true)}>
        Se oppgave
      </Button>
      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Body>
          <Heading size="medium" id="modal-heading">
            Oppgave fra Gosys
          </Heading>
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
              <BodyShort>{formaterFnr(fnr)}</BodyShort>
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
              <BodyShort>{hyphenIfNull(saksbehandler)}</BodyShort>
            </div>
          </InfoGrid>
          <BeskrivelseWrapper>
            <div>
              <Label>Beskrivelse</Label>
              <BodyShort>{beskrivelse ? beskrivelse : 'Ingen beskrivelse'}</BodyShort>
            </div>
          </BeskrivelseWrapper>
          <FlexRow justify="right">
            <Button variant="tertiary" onClick={() => setOpen(false)}>
              Avbryt
            </Button>
            <Button
              variant="primary"
              as="a"
              href={`${configContext['gosysUrl']}/personoversikt/fnr=${fnr}`}
              target="_blank"
            >
              Åpne og rediger i Gosys
            </Button>
          </FlexRow>
        </Modal.Body>
      </Modal>
    </>
  )
}

const hyphenIfNull = (inputString: string | null) => (inputString ? inputString : '-')
