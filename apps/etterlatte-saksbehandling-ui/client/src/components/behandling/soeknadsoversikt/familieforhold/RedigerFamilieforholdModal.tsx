import { Alert, Box, Button, Heading, HStack, Modal, TextField, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { PencilIcon, PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerFamilieforhold } from '~shared/api/behandling'
import { isPending, isSuccess, mapResult } from '~shared/api/apiUtils'
import { Personopplysninger } from '~shared/types/grunnlag'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useFieldArray, useForm } from 'react-hook-form'
import { fnrErGyldig } from '~utils/fnr'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { SakType } from '~shared/types/sak'
import { ApiErrorAlert } from '~ErrorBoundary'

type Props = {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger
}

// RHF krever at en array består av objekter
interface RHFRedigerbarFamilie {
  gjenlevende: Array<{ fnr: string }>
  avdoede: Array<{ fnr: string }>
}

export const RedigerFamilieforholdModal = ({ behandling, personopplysninger }: Props) => {
  const [isOpen, setIsOpen] = useState(false)
  const [status, redigerFamilieforholdRequest] = useApiCall(redigerFamilieforhold)

  const {
    control,
    formState: { errors },
    handleSubmit,
    getValues,
    register,
    reset,
  } = useForm<RHFRedigerbarFamilie>({
    defaultValues: {
      gjenlevende: personopplysninger.gjenlevende.map((gjenlevende) => ({
        fnr: gjenlevende.opplysning.foedselsnummer,
      })),
      avdoede: personopplysninger.avdoede.map((avdoede) => ({ fnr: avdoede.opplysning.foedselsnummer })),
    },
  })
  const gjenlevendeListe = useFieldArray({ name: 'gjenlevende', control })
  const avdoedListe = useFieldArray({ name: 'avdoede', control })

  const lagre = () => {
    const familieforhold = getValues()
    redigerFamilieforholdRequest(
      {
        behandlingId: behandling.id,
        redigert: {
          gjenlevende: familieforhold.gjenlevende.map((v) => v.fnr),
          avdoede: familieforhold.avdoede.map((v) => v.fnr),
        },
      },
      () => {
        setTimeout(() => window.location.reload(), 2000)
      }
    )
  }

  const avbryt = () => {
    reset()
    setIsOpen(false)
  }

  const kanLeggeTil = () => {
    const familie = getValues()
    return familie.gjenlevende.length + familie.avdoede.length < 2
  }

  return (
    <>
      <Button
        variant="secondary"
        size="small"
        onClick={() => setIsOpen(true)}
        icon={<PencilIcon aria-hidden />}
        iconPosition="right"
      >
        Rediger familieforhold
      </Button>

      <Modal open={isOpen} onClose={avbryt} aria-label="Rediger familieforhold">
        <Modal.Header>
          <Heading size="medium" spacing>
            Rediger familieforhold
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <FormWrapper $column={true}>
            <Box padding="space-4" borderWidth="1" borderColor="neutral-subtle">
              <VStack gap="space-4" align="start">
                {avdoedListe.fields?.map((field, index) => (
                  <HStack gap="space-2" key={field.id} align="end">
                    <Box width="20rem">
                      <TextField
                        {...register(`avdoede.${index}.fnr`, {
                          validate: {
                            fnrErGyldig: (value) => fnrErGyldig(value) || 'Ugyldig fødselsnummer',
                          },
                        })}
                        label={behandling.sakType === SakType.BARNEPENSJON ? 'Avdød forelder' : 'Avdød'}
                        description="Oppgi fødselsnummer"
                        error={errors?.avdoede?.[index]?.fnr?.message}
                      />
                    </Box>
                    <div>
                      <Button
                        icon={<XMarkIcon title="Fjern avdød" />}
                        variant="tertiary"
                        onClick={() => avdoedListe.remove(index)}
                      />
                    </div>
                  </HStack>
                ))}
                <Button
                  icon={<PlusIcon aria-hidden />}
                  onClick={() => avdoedListe.append({ fnr: '' })}
                  disabled={!kanLeggeTil() || isPending(status)}
                >
                  Legg til avdøde
                </Button>
              </VStack>
            </Box>

            <Box padding="space-4" borderWidth="1" borderColor="neutral-subtle">
              <VStack gap="space-4" align="start">
                {gjenlevendeListe.fields?.map((field, index) => (
                  <HStack gap="space-2" key={field.id} align="end">
                    <Box width="20rem">
                      <TextField
                        {...register(`gjenlevende.${index}.fnr`, {
                          validate: {
                            fnrErGyldig: (value) => fnrErGyldig(value) || 'Ugyldig fødselsnummer',
                          },
                        })}
                        label="Gjenlevende forelder"
                        description="Oppgi fødselsnummer"
                        error={errors?.gjenlevende?.[index]?.fnr?.message}
                      />
                    </Box>
                    <div>
                      <Button
                        icon={<XMarkIcon title="Fjern gjenlevende" />}
                        variant="tertiary"
                        onClick={() => gjenlevendeListe.remove(index)}
                      />
                    </div>
                  </HStack>
                ))}

                <Button
                  icon={<PlusIcon aria-hidden />}
                  onClick={() => gjenlevendeListe.append({ fnr: '' })}
                  disabled={!kanLeggeTil() || isPending(status)}
                >
                  Legg til gjenlevende
                </Button>
              </VStack>
            </Box>
          </FormWrapper>

          <br />

          {mapResult(status, {
            error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
            success: () => <Alert variant="success">Lagret redigert familieforhold</Alert>,
          })}
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="space-4" justify="end">
            <Button variant="secondary" onClick={avbryt} disabled={isPending(status)}>
              Avbryt
            </Button>
            <Button onClick={handleSubmit(lagre)} loading={isPending(status) || isSuccess(status)}>
              Lagre
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </>
  )
}
