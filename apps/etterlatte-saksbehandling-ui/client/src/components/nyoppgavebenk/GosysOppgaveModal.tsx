import { BodyShort, Button, Heading, Label, Modal } from '@navikt/ds-react'
import styled from 'styled-components'
import { EyeIcon } from '@navikt/aksel-icons'
import { useState } from 'react'
import { OppgavetypeTag, SaktypeTag } from '~components/nyoppgavebenk/Tags'
import { formaterFnr, formaterStringDato } from '~utils/formattering'
import { FristWrapper } from '~components/nyoppgavebenk/Oppgavelista'
import { isBefore } from 'date-fns'
import { hentGosysUrlApi, Oppgavestatus, Saktype } from '~shared/api/oppgaverny'

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
const gosysUrl = await hentGosysUrlApi().then((res) => {
  if (res.status === 'ok') {
    return res.data
  } else {
    throw Error(res.error)
  }
})

export const GosysOppgaveModal = (props: {
  oppgavestatus: Oppgavestatus
  fnr: string
  gjelder: string | null
  saktype: Saktype
  regdato: string
  fristdato: string
  beskrivelse: string | null
  enhet: string
  saksbehandler: string | null
}) => {
  const [open, setOpen] = useState(false)
  const { regdato, fristdato, oppgavestatus, fnr, gjelder, enhet, saksbehandler, beskrivelse, saktype } = props

  return (
    <>
      <Button variant="primary" size="small" icon={<EyeIcon />} onClick={() => setOpen(true)}>
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
              <BodyShort>{oppgavestatus}</BodyShort>
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
            <Button variant="tertiary" onClick={() => setOpen(false)}>
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

const hyphenIfNull = (inputString: string | null) => (inputString ? inputString : '-')
