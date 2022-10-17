import React, { FormEvent, useEffect, useState } from 'react'

import { Button, Checkbox, CheckboxGroup, Heading, Modal, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { useNavigate } from 'react-router-dom'
import { sendInnManueltOpphoer } from '../../shared/api/manueltOpphoer'

export const OPPHOERSGRUNNER = ['UTFLYTTING_FRA_NORGE', 'SOESKEN_DOED', 'GJENLEVENDE_FORELDER_DOED', 'ANNET'] as const

export type Opphoersgrunn = typeof OPPHOERSGRUNNER[number]

export const OVERSETTELSER_OPPHOERSGRUNNER: Record<Opphoersgrunn, string> = {
  GJENLEVENDE_FORELDER_DOED: 'Dødsfall gjenlevende forelder',
  SOESKEN_DOED: 'Dødsfall søsken',
  UTFLYTTING_FRA_NORGE: 'Utflytting fra Norge',
  ANNET: 'Annet',
}

export const ManueltOpphoerModal = ({ sakId }: { sakId: number }) => {
  const [open, setOpen] = useState(false)
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')
  const [selectedGrunner, setSelectedGrunner] = useState<Opphoersgrunn[]>([])
  const [feilmelding, setFeilmelding] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => setFeilmelding(''), [open])

  const erAnnetValgt = selectedGrunner.includes('ANNET')

  async function onSubmit(e: FormEvent) {
    e.preventDefault()

    setFeilmelding('')
    if (loading) {
      return
    }
    const grunn = fritekstgrunn.trim()
    if (erAnnetValgt && grunn.length === 0) {
      setFeilmelding('Du må beskrive hvorfor du opphører hvis du har valgt "Annet"')
      return
    }
    if (selectedGrunner.length === 0) {
      setFeilmelding('Du må oppgi en grunn for det manuelle opphøret')
      return
    }

    setLoading(true)
    const kjenteAarsaker = selectedGrunner.filter((grunn) => grunn !== 'ANNET')
    const res = await sendInnManueltOpphoer(sakId!, kjenteAarsaker, grunn)
    if (res.status === 'ok') {
      navigate(`/behandling/${res.data.behandlingId}/`)
    } else {
      setFeilmelding('Kunne ikke annullere denne saken nå. Prøv igjen senere, eller meld det som feil i systemet.')
    }
    setLoading(false)
  }

  return (
    <>
      <Button variant="secondary" onClick={() => setOpen(true)}>
        Annuller saken
      </Button>
      <Modal open={open} onClose={() => setOpen(false)}>
        <Modal.Content>
          <ModalSpacing>
            <Heading size="large">Manuelt opphør</Heading>
            <p>Hvis du annullerer vil utbetalinger fjernes fra oppdragssystemet og du må behandle saken i PeSys</p>
            <p>Følgende utbetalinger annulleres:</p>
            <p>TODO</p>
            <form onSubmit={onSubmit}>
              <GrunnerTilAnnuleringForm
                erAnnetValgt={erAnnetValgt}
                fritekstgrunn={fritekstgrunn}
                setFritekstgrunn={setFritekstgrunn}
                setSelectedGrunner={setSelectedGrunner}
              />
              {feilmelding.length > 0 ? <Feilmelding>{feilmelding}</Feilmelding> : null}
              <FormKnapper>
                <Button loading={loading} type="submit">
                  Opphør denne saken
                </Button>
                <Button variant="secondary" onClick={() => setOpen(false)}>
                  Avbryt
                </Button>
              </FormKnapper>
            </form>
          </ModalSpacing>
        </Modal.Content>
      </Modal>
    </>
  )
}

const Feilmelding = styled.p`
  color: var(--navds-semantic-color-feedback-danger-text);
`

const FormKnapper = styled.div`
  margin-top: 1rem;
  display: flex;
  flex-direction: row-reverse;
  gap: 1rem;
`

const ModalSpacing = styled.div`
  padding: 1rem;
`

const GrunnerTilAnnuleringForm = ({
  fritekstgrunn,
  erAnnetValgt,
  setFritekstgrunn,
  setSelectedGrunner,
}: {
  fritekstgrunn: string
  erAnnetValgt: boolean
  setFritekstgrunn: (grunn: string) => void
  setSelectedGrunner: (grunner: Opphoersgrunn[]) => void
}) => {
  return (
    <FormWrapper>
      <CheckboxGroup legend="Hvorfor må saken annulleres?" onChange={setSelectedGrunner}>
        {OPPHOERSGRUNNER.map((opphoersgrunn) => (
          <Checkbox value={opphoersgrunn} key={opphoersgrunn}>
            {OVERSETTELSER_OPPHOERSGRUNNER[opphoersgrunn]}
          </Checkbox>
        ))}
      </CheckboxGroup>
      {erAnnetValgt ? (
        <TextField
          label="Beskriv hvorfor"
          size="small"
          type="text"
          value={fritekstgrunn}
          onChange={(e) => setFritekstgrunn(e.target.value)}
        />
      ) : null}
    </FormWrapper>
  )
}

const FormWrapper = styled.div`
  max-width: 20rem;
  display: flex;
  gap: 1rem;
  flex-direction: column;
`
