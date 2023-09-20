import React, { FormEvent, useEffect, useState } from 'react'

import { Button, Checkbox, CheckboxGroup, Heading, Modal, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { useNavigate } from 'react-router-dom'
import { sendInnManueltOpphoer } from '~shared/api/manueltOpphoer'
import { formaterBehandlingstype, formaterKanskjeStringDato } from '~utils/formattering'
import { IBehandlingsammendrag } from '~components/person/typer'
import { harIngenUavbrutteManuelleOpphoer, kunIverksatteBehandlinger } from '~components/behandling/felles/utils'

export const OPPHOERSGRUNNER = [
  'SOEKER_DOED',
  'UTFLYTTING_FRA_NORGE',
  'SOESKEN_DOED',
  'GJENLEVENDE_FORELDER_DOED',
  'ADOPSJON',
  'FENGSELSOPPHOLD',
  'INSTITUSJONSOPPHOLD',
  'FORSOERGER_FAAR_FORELDREANSVAR',
  'ANNET',
] as const

export type Opphoersgrunn = (typeof OPPHOERSGRUNNER)[number]

export const OVERSETTELSER_OPPHOERSGRUNNER: Record<Opphoersgrunn, string> = {
  SOEKER_DOED: 'Dødsfall søker',
  GJENLEVENDE_FORELDER_DOED: 'Dødsfall gjenlevende forelder',
  SOESKEN_DOED: 'Dødsfall søsken',
  UTFLYTTING_FRA_NORGE: 'Utflytting fra Norge',
  ADOPSJON: 'Adopsjon',
  INSTITUSJONSOPPHOLD: 'Institusjonsopphold',
  FENGSELSOPPHOLD: 'Fengselsopphold',
  FORSOERGER_FAAR_FORELDREANSVAR: 'Enslig forsørger har fått foreldreansvar',
  ANNET: 'Annet',
}

export const ManueltOpphoerModal = ({
  sakId,
  behandlingliste,
}: {
  sakId: number
  behandlingliste: IBehandlingsammendrag[]
}) => {
  const [open, setOpen] = useState(false)
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')
  const [selectedGrunner, setSelectedGrunner] = useState<Opphoersgrunn[]>([])
  const [feilmelding, setFeilmelding] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => setFeilmelding(''), [open])

  const erAnnetValgt = selectedGrunner.includes('ANNET')

  const iverksatteBehandlinger = kunIverksatteBehandlinger(behandlingliste)
  const kanOppretteManueltOpphoer =
    iverksatteBehandlinger.length > 0 && harIngenUavbrutteManuelleOpphoer(behandlingliste)

  if (!kanOppretteManueltOpphoer) {
    return null
  }

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
    const res = await sendInnManueltOpphoer(sakId, kjenteAarsaker, grunn)
    if (res.status === 'ok') {
      navigate(`/behandling/${res.data.behandlingId}/`)
    } else {
      setFeilmelding('Kunne ikke annullere denne saken nå. Prøv igjen senere, eller meld det som feil i systemet.')
    }
    setLoading(false)
  }

  return (
    <>
      <Button variant="danger" onClick={() => setOpen(true)} size="small">
        Annuller saken
      </Button>

      <Modal open={open} onClose={() => setOpen(false)}>
        <Modal.Body>
          <ModalSpacing>
            <Heading size="large">Manuelt opphør</Heading>
            <p>Hvis du annullerer vil utbetalinger fjernes fra oppdragssystemet og du må behandle saken i Pesys</p>
            <p>Utbetalinger fra følgende behandlinger annulleres:</p>
            <OppsummeringListe>
              {iverksatteBehandlinger.map((behandling) => (
                <li key={behandling.id}>
                  {formaterBehandlingstype(behandling.behandlingType)} med virkningstidspunkt{' '}
                  {formaterKanskjeStringDato(behandling.virkningstidspunkt?.dato)}
                </li>
              ))}
            </OppsummeringListe>
            <form onSubmit={onSubmit}>
              <GrunnerTilAnnuleringForm
                erAnnetValgt={erAnnetValgt}
                fritekstgrunn={fritekstgrunn}
                setFritekstgrunn={setFritekstgrunn}
                setSelectedGrunner={setSelectedGrunner}
              />
              {feilmelding.length > 0 ? <Feilmelding>{feilmelding}</Feilmelding> : null}
              <FormKnapper>
                <Button loading={loading} type="submit" variant="danger">
                  Opphør denne saken
                </Button>
                <Button variant="secondary" onClick={() => setOpen(false)}>
                  Avbryt
                </Button>
              </FormKnapper>
            </form>
          </ModalSpacing>
        </Modal.Body>
      </Modal>
    </>
  )
}

const Feilmelding = styled.p`
  color: var(--navds-error-message-color-text);
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

const OppsummeringListe = styled.ul`
  margin: 0 0 2em 2em;
`
