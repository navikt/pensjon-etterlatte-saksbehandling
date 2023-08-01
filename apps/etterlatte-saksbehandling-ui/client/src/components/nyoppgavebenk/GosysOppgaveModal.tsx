import { BodyShort, Button, Heading, Label, Modal } from '@navikt/ds-react'
import styled from 'styled-components'
import { EyeIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { formaterFnr, formaterStringDato } from '~utils/formattering'
import { FristWrapper } from '~components/nyoppgavebenk/Oppgavelista'
import { isBefore } from 'date-fns'
import { SakType } from '~shared/types/sak'

const GosysOppgaveMock = {
  regdato: '2023-07-27T09:25:08.802267',
  fristdato: '2023-08-27T00:00',
  status: 'OPPRETTET',
  fnr: '09031355831',
  gjelder: 'Vurder konsekvens',
  enhet: '4808',
  saksbehandler: '',
  beskrivelse:
    'Lenke videre til gosys for mer detaljer om oppgaven.' +
    'Informere om at endringer (oppdateringer, endre frist, ferdigstille osv) på oppgave må gjøres i Gosys.',
  saktype: SakType.OMSTILLINGSSTOENAD,
}

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

const ButtonRow = styled.div`
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
`

export const GosysOppgaveModal = () => {
  const [open, setOpen] = useState(false)
  const gosysUrl = process.env.GOSYS_URL

  // TODO: Hent dette fra props
  const { regdato, fristdato, status, fnr, gjelder, enhet, saksbehandler, beskrivelse, saktype } = GosysOppgaveMock

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon />} onClick={() => setOpen(!open)}>
        Se oppgave
      </Button>
      <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)}>
        <Modal.Content>
          <Heading size="medium" id="modal-heading">
            Oppgave fra Gosys
          </Heading>
          <TagRow>
            <SaktypeTag sakType={saktype} />
            <OppgavetypeTag oppgavetype="GOSYS" />
          </TagRow>
          <InfoGrid>
            <div>
              <Label>Reg.dato</Label>
              <BodyShort>{formaterStringDato(regdato)}</BodyShort>
            </div>
            <div>
              <Label>Frist</Label>
              <BodyShort>
                <FristWrapper fristHarPassert={!!fristdato && isBefore(new Date(fristdato), new Date())}>
                  {fristdato ? formaterStringDato(fristdato) : 'Ingen frist'}
                </FristWrapper>
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
              <BodyShort>{beskrivelse}</BodyShort>
            </div>
          </BeskrivelseWrapper>
          <ButtonRow>
            <Button variant="tertiary" onClick={() => setOpen(!open)}>
              Avbryt
            </Button>
            <Button variant="primary" as="a" href={`${gosysUrl}/personoversikt/fnr=${fnr}`} target="_blank">
              Åpne og rediger i Gosys
            </Button>
          </ButtonRow>
        </Modal.Content>
      </Modal>
    </>
  )
}

const hyphenIfNull = (inputString: string) => (inputString ? inputString : '-')
