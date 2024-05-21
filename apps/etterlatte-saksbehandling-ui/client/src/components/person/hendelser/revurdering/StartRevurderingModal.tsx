import React, { ReactNode, useState } from 'react'
import { Grunnlagsendringshendelse } from '~components/person/typer'
import { Alert, Button, Heading, Modal, Select, TextField, VStack } from '@navikt/ds-react'
import { ArrowsCirclepathIcon } from '@navikt/aksel-icons'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import { isPending } from '~shared/api/apiUtils'
import styled from 'styled-components'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'
import { ButtonGroup } from '~shared/styled'

interface Props {
  hendelse: Grunnlagsendringshendelse
  sakId: number
  revurderinger: Array<Revurderingaarsak>
}

export const StartRevurderingModal = ({ hendelse, sakId, revurderinger }: Props): ReactNode => {
  const navigate = useNavigate()

  const [open, setOpen] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)

  const [valgtAarsak, setValgtAarsak] = useState<Revurderingaarsak | undefined>(undefined)
  const [fritekstgrunn, setFritekstgrunn] = useState<string>('')

  const [opprettRevurderingResult, opprettRevurdering] = useApiCall(opprettRevurderingApi)

  const onSubmit = () => {
    if (valgtAarsak === undefined) {
      return setError('Du må velge en årsak')
    }

    opprettRevurdering(
      {
        sakId: sakId,
        aarsak: valgtAarsak,
        paaGrunnAvHendelseId: hendelse.id,
        fritekstAarsak: fritekstgrunn,
      },
      (revurderingId: string) => {
        navigate(`/behandling/${revurderingId}/`)
      },
      () => {
        setError('En feil skjedde ved opprettelse av revurderingen. Prøv igjen senere.')
      }
    )
  }

  return (
    <>
      <Button size="small" icon={<ArrowsCirclepathIcon aria-hidden />} onClick={() => setOpen(true)}>
        Start revurdering
      </Button>

      <Modal open={open} onClose={() => setOpen(false)} aria-label="Vurder hendelse">
        <Modal.Body>
          <Heading spacing size="large">
            Vurder hendelse
          </Heading>
          <div>
            <Select
              label="Velg revurderingsårsak"
              value={valgtAarsak}
              onChange={(e) => setValgtAarsak(e.target.value as Revurderingaarsak)}
              error={error}
            >
              <option value="">Velg en årsak</option>
              {revurderinger.map((aarsak) => (
                <option value={aarsak} key={aarsak}>
                  {tekstRevurderingsaarsak[aarsak]}
                </option>
              ))}
            </Select>
            {valgtAarsak === Revurderingaarsak.ANNEN && (
              <AnnenRevurderingWrapper gap="5">
                <TextField
                  label="Beskriv årsak"
                  size="medium"
                  type="text"
                  value={fritekstgrunn}
                  onChange={(e) => setFritekstgrunn(e.target.value)}
                />
                <AnnenRevurderingAlert variant="warning" size="small" inline>
                  Bruk denne årsaken kun dersom andre årsaker ikke er dekkende for revurderingen.
                </AnnenRevurderingAlert>
              </AnnenRevurderingWrapper>
            )}
          </div>

          <ButtonGroup>
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Avbryt
            </Button>
            <Button loading={isPending(opprettRevurderingResult)} onClick={onSubmit}>
              Start revurdering
            </Button>
          </ButtonGroup>
        </Modal.Body>
      </Modal>
    </>
  )
}

const AnnenRevurderingWrapper = styled(VStack)`
  margin-top: 1rem;
`

const AnnenRevurderingAlert = styled(Alert)`
  max-width: 20rem;
`
