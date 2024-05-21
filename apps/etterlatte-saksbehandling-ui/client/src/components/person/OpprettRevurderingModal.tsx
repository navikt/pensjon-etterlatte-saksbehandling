import React, { useEffect, useState } from 'react'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentStoettedeRevurderinger, opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'
import { isPending, mapFailure, mapResult } from '~shared/api/apiUtils'
import { Alert, Button, Heading, Loader, Modal, Select, TextField, VStack } from '@navikt/ds-react'
import { ArrowCirclepathIcon } from '@navikt/aksel-icons'
import { useForm } from 'react-hook-form'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import styled from 'styled-components'
import { ButtonGroup } from '~shared/styled'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useNavigate } from 'react-router-dom'

interface OpprettRevurderingSkjema {
  aarsak: Revurderingaarsak
  fritekstAarsak?: string
}

interface Props {
  sakId: number
  sakType: SakType
  begrunnelse?: string
  hendelseId?: string
  oppgaveId?: string
}

export const OpprettRevurderingModal = ({ sakId, sakType, begrunnelse, hendelseId, oppgaveId }: Props) => {
  const navigate = useNavigate()

  const [aapen, setAapen] = useState<boolean>(false)

  const [muligeRevurderingAarsakerResult, muligeRevurderingeraarsakerFetch] = useApiCall(hentStoettedeRevurderinger)
  const [opprettRevurderingResult, opprettRevurdering, resetApiCall] = useApiCall(opprettRevurderingApi)

  const {
    register,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<OpprettRevurderingSkjema>()

  const paaOpprett = ({ aarsak, fritekstAarsak }: OpprettRevurderingSkjema) => {
    opprettRevurdering(
      {
        sakId,
        begrunnelse,
        aarsak,
        fritekstAarsak,
        paaGrunnAvHendelseId: hendelseId,
        paaGrunnAvOppgaveId: oppgaveId,
      },
      (revurderingId: string) => navigate(`/behandling/${revurderingId}/`)
    )
  }

  const lukkModal = () => {
    resetApiCall()
    setAapen(false)
  }

  useEffect(() => {
    muligeRevurderingeraarsakerFetch({ sakType })
  }, [])

  return mapResult(muligeRevurderingAarsakerResult, {
    pending: <Loader />,
    success: (muligeRevurderingAarsaker) =>
      !!muligeRevurderingAarsaker?.length ? (
        <>
          <Button
            variant={oppgaveId || hendelseId ? 'primary' : 'secondary'}
            size={oppgaveId || hendelseId ? 'small' : 'medium'}
            icon={hendelseId && <ArrowCirclepathIcon aria-hidden />}
            onClick={() => setAapen(true)}
          >
            Opprett revurdering
          </Button>
          <Modal open={aapen} onClose={lukkModal} aria-labelledby="Opprett revurdering modal">
            <Modal.Header closeButton>
              <Heading level="2" size="medium">
                Opprett revurdering
              </Heading>
            </Modal.Header>
            <Modal.Body>
              <Select
                {...register('aarsak', {
                  required: {
                    value: true,
                    message: 'Du må velge en årsak for revurdering',
                  },
                })}
                label="Årsak til revurdering"
                error={errors.aarsak?.message}
              >
                <option value="">Velg årsak</option>
                {muligeRevurderingAarsaker.map((aarsak, index) => (
                  <option key={index} value={aarsak}>
                    {tekstRevurderingsaarsak[aarsak]}
                  </option>
                ))}
              </Select>

              {watch().aarsak === Revurderingaarsak.ANNEN && (
                <AnnenRevurderingWrapper gap="4">
                  <TextField {...register('fritekstAarsak')} label="Beskriv årsak" />
                  <AnnenRevurderingAlert variant="warning" size="small" inline>
                    Bruk denne årsaken kun dersom andre årsaker ikke er dekkende for revurderingen.
                  </AnnenRevurderingAlert>
                </AnnenRevurderingWrapper>
              )}

              {mapFailure(opprettRevurderingResult, (error) => (
                <ApiErrorAlert>{error.detail || 'Kunne ikke opprette revurdering'}</ApiErrorAlert>
              ))}

              <ButtonGroup>
                <Button variant="secondary" type="button" onClick={lukkModal}>
                  Avbryt
                </Button>
                <Button loading={isPending(opprettRevurderingResult)} onClick={handleSubmit(paaOpprett)}>
                  Opprett
                </Button>
              </ButtonGroup>
            </Modal.Body>
          </Modal>
        </>
      ) : (
        <Alert variant="info" inline>
          Ingen mulige revurderinger for denne saken
        </Alert>
      ),
  })
}

const AnnenRevurderingWrapper = styled(VStack)`
  margin-top: 1rem;
`

const AnnenRevurderingAlert = styled(Alert)`
  max-width: 20rem;
`
