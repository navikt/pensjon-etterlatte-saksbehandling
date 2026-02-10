import React, { useEffect, useState } from 'react'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentStoettedeRevurderinger, opprettRevurdering as opprettRevurderingApi } from '~shared/api/revurdering'
import { isPending, mapFailure, mapResult } from '~shared/api/apiUtils'
import { Alert, Button, Heading, HStack, Modal, Select, TextField, VStack } from '@navikt/ds-react'
import { ArrowCirclepathIcon } from '@navikt/aksel-icons'
import { useForm } from 'react-hook-form'
import { Revurderingaarsak, tekstRevurderingsaarsak } from '~shared/types/Revurderingaarsak'
import styled from 'styled-components'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useNavigate } from 'react-router-dom'
import Spinner from '~shared/Spinner'

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

function sorterRevurderingerKronologisk(revurderinger: Revurderingaarsak[]): Revurderingaarsak[] {
  return revurderinger.toSorted((first, last) => {
    if (tekstRevurderingsaarsak[first].trim().toLowerCase() > tekstRevurderingsaarsak[last].trim().toLowerCase()) {
      return 1
    }
    return -1
  })
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
    if (aapen) muligeRevurderingeraarsakerFetch({ sakType })
  }, [aapen])

  return (
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
          {mapResult(muligeRevurderingAarsakerResult, {
            pending: <Spinner label="Henter revurderingsårsaker..." />,
            success: (muligeRevurderingAarsaker) => {
              const sorterteMuligeRevurderingAarsaker = sorterRevurderingerKronologisk(muligeRevurderingAarsaker)
              return (
                <>
                  {sorterteMuligeRevurderingAarsaker.length ? (
                    <VStack gap="space-4">
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
                        {sorterteMuligeRevurderingAarsaker.map((aarsak, index) => (
                          <option key={index} value={aarsak}>
                            {tekstRevurderingsaarsak[aarsak]}
                          </option>
                        ))}
                      </Select>
                      {watch().aarsak == Revurderingaarsak.SLUTTBEHANDLING && (
                        <Alert variant="warning">
                          Sluttbehandling kan nå opprettes uten å huket av for skal sende kravpakke, dette for å støtte
                          kravpakke som finnes i feks PESYS.
                        </Alert>
                      )}
                      {watch().aarsak === Revurderingaarsak.ANNEN && (
                        <AnnenRevurderingWrapper gap="space-4">
                          <TextField
                            {...register('fritekstAarsak', {
                              required: {
                                value: true,
                                message: 'Du må beskrive årsaken',
                              },
                            })}
                            label="Beskriv årsak"
                            error={errors.fritekstAarsak?.message}
                          />
                          <AnnenRevurderingAlert variant="warning" size="small" inline>
                            Bruk denne årsaken kun dersom andre årsaker ikke er dekkende for revurderingen.
                          </AnnenRevurderingAlert>
                        </AnnenRevurderingWrapper>
                      )}

                      {mapFailure(opprettRevurderingResult, (error) => (
                        <ApiErrorAlert>{error.detail || 'Kunne ikke opprette revurdering'}</ApiErrorAlert>
                      ))}

                      <HStack gap="space-2" justify="end">
                        <Button variant="secondary" type="button" onClick={lukkModal}>
                          Avbryt
                        </Button>
                        <Button loading={isPending(opprettRevurderingResult)} onClick={handleSubmit(paaOpprett)}>
                          Opprett
                        </Button>
                      </HStack>
                    </VStack>
                  ) : (
                    <Alert variant="info" size="small">
                      Ingen revurderingsårsaker for denne saken
                    </Alert>
                  )}
                </>
              )
            },
          })}
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
