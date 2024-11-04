import {
  Alert,
  Button,
  Heading,
  HGrid,
  HStack,
  Label,
  Modal,
  Radio,
  Select,
  TextField,
  ToggleGroup,
  VStack,
} from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { AdresseType, Mottaker } from '~shared/types/Brev'
import { isPending, Result } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Controller, useForm } from 'react-hook-form'
import { DocPencilIcon } from '@navikt/aksel-icons'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'

enum MottakerType {
  PRIVATPERSON = 'PRIVATPERSON',
  BEDRIFT = 'BEDRIFT',
}

interface Props {
  brevId: number
  sakId: number
  mottaker: Mottaker
  lagre: (brevId: number, sakId: number, mottaker: Mottaker, onSuccess: () => void) => void
  lagreResult: Result<void>
}

export function RedigerMottakerModal({ brevId, sakId, mottaker: initialMottaker, lagre, lagreResult }: Props) {
  const [isOpen, setIsOpen] = useState(false)
  const [mottakerType, setMottakerType] = useState(
    initialMottaker.orgnummer ? MottakerType.BEDRIFT : MottakerType.PRIVATPERSON
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

  const lagreEndringer = (mottaker: Mottaker) => {
    if (!isDirty) {
      setIsOpen(false)
      return
    }

    lagre(brevId, sakId, mottaker, () => {
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

      <form onSubmit={handleSubmit(lagreEndringer)}>
        <Modal open={isOpen} onClose={avbryt} width="medium" aria-label="Endre mottaker">
          <Modal.Body>
            <VStack gap="4">
              <Heading size="large" spacing>
                Endre mottaker
              </Heading>

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

              {!erNorskAdresse && (
                <Alert variant="warning" size="small">
                  OBS: På utenlandske adresser må du kun bruke adresselinjer, land og landkode
                </Alert>
              )}

              <VStack>
                <Label>Adresselinjer</Label>
                <TextField
                  {...register('adresse.adresselinje1', {
                    required: {
                      value: true,
                      message: 'Det må være minst én adresselinje',
                    },
                  })}
                  label=""
                  placeholder="Adresselinje 1"
                  error={errors?.adresse?.adresselinje1?.message}
                />
                <TextField {...register('adresse.adresselinje2')} label="" placeholder="Adresselinje 2" />
                <TextField {...register('adresse.adresselinje3')} label="" placeholder="Adresselinje 3" />
              </VStack>

              {erNorskAdresse && (
                <HGrid columns={2} gap="4 4">
                  <TextField
                    {...register('adresse.postnummer', {
                      shouldUnregister: true,
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
                      shouldUnregister: true,
                      required: {
                        value: erNorskAdresse,
                        message: 'Poststed må være satt på norsk adresse',
                      },
                    })}
                    label="Poststed"
                    error={errors?.adresse?.poststed?.message}
                  />
                </HGrid>
              )}

              <HGrid columns={2} gap="4 4">
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

              <ControlledRadioGruppe
                name="tvingSentralPrint"
                control={control}
                legend="Distribusjonsmetode"
                description="Automatisk er anbefalt ettersom tjenesten da tar hensyn til brukerens valg i KRR. Dersom
                det er behov for utsending av fysisk brev kan du tvinge sentral print."
                radios={
                  <>
                    <Radio value={false}>Automatisk</Radio>
                    <Radio value={true}>Tving sentral print</Radio>
                  </>
                }
              />

              {isFailureHandler({
                apiResult: lagreResult,
                errorMessage: 'Kunne ikke oppdatere mottaker.',
              })}

              <HStack gap="4" justify="end">
                <Button variant="secondary" type="button" disabled={isPending(lagreResult)} onClick={avbryt}>
                  Avbryt
                </Button>
                <Button variant="primary" type="submit" loading={isPending(lagreResult)}>
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
