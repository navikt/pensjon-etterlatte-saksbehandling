import { Alert, Box, Button, Heading, HStack, Modal, TextField } from '@navikt/ds-react'
import { InputList, InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import React, { useState } from 'react'
import { PencilIcon, PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerFamilieforhold } from '~shared/api/behandling'
import { isPending, isSuccess, mapFailure } from '~shared/api/apiUtils'
import { Personopplysninger } from '~shared/types/grunnlag'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useFieldArray, useForm } from 'react-hook-form'
import { fnrErGyldig } from '~utils/fnr'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import { SakType } from '~shared/types/sak'

type Props = {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger
}

// RHF krever at en array består av objekter
interface RHFRedigerbarFamilie {
  gjenlevende: Array<{ fnr: string }>
  avdoede: Array<{ fnr: string }>
}

export const RedigerFamilieforhold = ({ behandling, personopplysninger }: Props) => {
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
        variant="tertiary"
        size="small"
        onClick={() => setIsOpen(true)}
        icon={<PencilIcon />}
        style={{ float: 'right' }}
      >
        Rediger
      </Button>

      <Modal open={isOpen} onClose={avbryt} aria-label="Rediger familieforhold">
        <Modal.Header>
          <Heading size="medium" spacing>
            Rediger familieforhold
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <FormWrapper $column={true}>
            <Box padding="4" borderWidth="1" borderRadius="small">
              <InputList>
                {avdoedListe.fields?.map((field, index) => (
                  <InputRow key={field.id}>
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
                    <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => avdoedListe.remove(index)} />
                  </InputRow>
                ))}
                <Button
                  icon={<PlusIcon />}
                  onClick={() => avdoedListe.append({ fnr: '' })}
                  disabled={!kanLeggeTil() || isPending(status)}
                >
                  Legg til avdøde
                </Button>
              </InputList>
            </Box>

            <Box padding="4" borderWidth="1" borderRadius="small">
              <InputList>
                {gjenlevendeListe.fields?.map((field, index) => (
                  <InputRow key={field.id}>
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
                    <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => gjenlevendeListe.remove(index)} />
                  </InputRow>
                ))}

                <Button
                  icon={<PlusIcon />}
                  onClick={() => gjenlevendeListe.append({ fnr: '' })}
                  disabled={!kanLeggeTil() || isPending(status)}
                >
                  Legg til gjenlevende
                </Button>
              </InputList>
            </Box>
          </FormWrapper>

          <br />

          {isSuccess(status) && <Alert variant="success">Lagret redigert familieforhold</Alert>}
          {mapFailure(status, (error) => (
            <Alert variant="error">{error.detail}</Alert>
          ))}
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="4" justify="end">
            <Button variant="secondary" onClick={avbryt} disabled={isPending(status)}>
              Avbryt
            </Button>
            <Button onClick={handleSubmit(lagre)} loading={isPending(status)}>
              Lagre
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </>
  )
}
