import { Button, Heading, HGrid, HStack, Modal, Select, TextField, ToggleGroup, VStack } from '@navikt/ds-react'
import React, { Dispatch, SetStateAction, useEffect, useState } from 'react'
import { AdresseType, IBrev, Mottaker } from '~shared/types/Brev'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterMottaker } from '~shared/api/brev'
import { isPending, Result } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Controller, useForm } from 'react-hook-form'
import { DocPencilIcon } from '@navikt/aksel-icons'
import { VergeadressePanel } from '~components/person/brev/mottaker/VergeadressePanel'

enum MottakerType {
  PRIVATPERSON = 'PRIVATPERSON',
  BEDRIFT = 'BEDRIFT',
}

interface Props {
  brev: IBrev
  setBrev: Dispatch<SetStateAction<IBrev>>
  vergeadresse: Result<Mottaker>
}

export function BrevMottakerModal({ brev, setBrev, vergeadresse }: Props) {
  const { id: brevId, sakId, mottaker: initialMottaker } = brev

  const [mottakerStatus, apiOppdaterMottaker] = useApiCall(oppdaterMottaker)

  const [isOpen, setIsOpen] = useState(false)
  const [mottakerType, setMottakerType] = useState(
    brev.mottaker.orgnummer ? MottakerType.BEDRIFT : MottakerType.PRIVATPERSON
  )

  const {
    control,
    formState: { errors, isDirty },
    handleSubmit,
    register,
    reset,
    setValue,
    watch,
  } = useForm<Mottaker>({
    defaultValues: initialMottaker,
  })

  useEffect(() => {
    if (mottakerType == MottakerType.BEDRIFT) setValue('foedselsnummer', undefined)
    else if (mottakerType == MottakerType.PRIVATPERSON) setValue('orgnummer', undefined)
  }, [mottakerType])

  const lagre = (mottaker: Mottaker) => {
    if (!isDirty) {
      setIsOpen(false)
      return
    }

    apiOppdaterMottaker({ brevId, sakId, mottaker: mottaker }, () => {
      setBrev({ ...brev, mottaker })
      setIsOpen(false)
    })
  }

  const avbryt = () => {
    reset(initialMottaker)
    setIsOpen(false)
  }

  const erNorskAdresse = watch('adresse.adresseType') === AdresseType.NORSKPOSTADRESSE

  return (
    <>
      <Button variant="secondary" onClick={() => setIsOpen(true)} icon={<DocPencilIcon aria-hidden />} size="small" />

      <form onSubmit={handleSubmit((data) => lagre(data))}>
        <Modal open={isOpen} onClose={avbryt} width="medium" aria-label="Endre mottaker">
          <Modal.Body>
            <VStack gap="4">
              <Heading size="large" spacing>
                Endre mottaker
              </Heading>

              <VergeadressePanel vergeadresseResult={vergeadresse} />

              <ToggleGroup
                defaultValue={mottakerType}
                onChange={(value) => setMottakerType(value as MottakerType)}
                size="small"
              >
                <ToggleGroup.Item value={MottakerType.PRIVATPERSON}>Privatperson</ToggleGroup.Item>
                <ToggleGroup.Item value={MottakerType.BEDRIFT}>Bedrift</ToggleGroup.Item>
              </ToggleGroup>

              {mottakerType == MottakerType.BEDRIFT && (
                <TextField
                  {...register('orgnummer', {
                    required: {
                      value: true,
                      message: 'Orgnummer må være satt når mottaker er bedrift',
                    },
                    pattern: {
                      value: /^\d{9}$/,
                      message: 'Et gyldig orgnummer har kun ni siffer',
                    },
                  })}
                  label="Orgnummer"
                  error={errors?.orgnummer?.message}
                />
              )}
              {mottakerType == MottakerType.PRIVATPERSON && (
                <TextField
                  {...register('foedselsnummer.value', {
                    required: {
                      value: true,
                      message: 'Fødselsnummer må være satt',
                    },
                    pattern: {
                      value: /^\d{11}$/,
                      message: 'Fødselsnummer kan kun bestå av 11 siffer',
                    },
                  })}
                  label="Fødselsnummer"
                  error={errors?.foedselsnummer?.value?.message}
                />
              )}

              <TextField
                {...register('navn', {
                  required: {
                    value: true,
                    message: 'Navn må være satt',
                  },
                })}
                label="Navn"
                error={errors?.navn?.message}
              />

              <Controller
                control={control}
                name="adresse.adresseType"
                rules={{
                  required: {
                    value: true,
                    message: 'Adressetype må være satt',
                  },
                }}
                render={({ field, formState: { errors } }) => (
                  <Select {...field} label="Adressetype" error={errors?.adresse?.adresseType?.message}>
                    <option></option>
                    <option value={AdresseType.NORSKPOSTADRESSE}>Norsk postadresse</option>
                    <option value={AdresseType.UTENLANDSKPOSTADRESSE}>Utenlandsk postadresse</option>
                  </Select>
                )}
              />

              <TextField
                {...register('adresse.adresselinje1', {
                  required: {
                    value: true,
                    message: 'Det må være minst én adresselinje',
                  },
                })}
                label="Adresselinje 1"
                error={errors?.adresse?.adresselinje1?.message}
              />
              <TextField {...register('adresse.adresselinje2')} label="Adresselinje 2" />
              <TextField {...register('adresse.adresselinje3')} label="Adresselinje 3" />

              <HGrid columns={2} gap="4 4">
                <TextField
                  {...register('adresse.postnummer', {
                    required: {
                      value: erNorskAdresse,
                      message: 'Postnummer må være satt på norsk adresse',
                    },
                    pattern: erNorskAdresse
                      ? {
                          value: /^\d{4}$/,
                          message: 'Ugyldig tegn i postnummeret. Norske postnummer kan kun bestå av fire siffer.',
                        }
                      : undefined,
                  })}
                  label="Postnummer"
                  error={errors?.adresse?.postnummer?.message}
                />
                <TextField
                  {...register('adresse.poststed', {
                    required: {
                      value: erNorskAdresse,
                      message: 'Poststed må være satt på norsk adresse',
                    },
                  })}
                  label="Poststed"
                  error={errors?.adresse?.poststed?.message}
                />

                <TextField
                  {...register('adresse.landkode', {
                    required: {
                      value: true,
                      message: 'Landkode må være satt (2 tegn)',
                    },
                    pattern: {
                      value: /^[A-Z]{2}$/,
                      message: 'Ugyldig tegn i landkoden. Landkode skal kun bestå av 2 store bokstaver (eks. NO)',
                    },
                  })}
                  label="Landkode"
                  error={errors?.adresse?.landkode?.message}
                />
                <TextField
                  {...register('adresse.land', {
                    required: {
                      value: true,
                      message: 'Land må være satt',
                    },
                  })}
                  label="Land"
                  error={errors?.adresse?.land?.message}
                />
              </HGrid>
              {isFailureHandler({
                apiResult: mottakerStatus,
                errorMessage: 'Kunne ikke oppdatere mottaker.',
              })}

              <HStack gap="4" justify="end">
                <Button variant="secondary" type="button" disabled={isPending(mottakerStatus)} onClick={avbryt}>
                  Avbryt
                </Button>
                <Button variant="primary" type="submit" loading={isPending(mottakerStatus)}>
                  Lagre
                </Button>
              </HStack>
            </VStack>
          </Modal.Body>
        </Modal>
      </form>
    </>
  )
}
