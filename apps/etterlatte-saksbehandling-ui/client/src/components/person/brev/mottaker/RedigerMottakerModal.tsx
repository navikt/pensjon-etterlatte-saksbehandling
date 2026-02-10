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
  UNSAFE_Combobox,
  VStack,
} from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { AdresseType, Mottaker } from '~shared/types/Brev'
import { isInitial, isPending, mapResult, Result } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { Controller, useForm } from 'react-hook-form'
import { DocPencilIcon } from '@navikt/aksel-icons'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentKodeverkLandISO2 } from '~shared/api/kodeverk'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { capitalize } from '~utils/formatering/formatering'

enum JuridiskEnhet {
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
  const [juridiskEnhet, setJuridiskEnhet] = useState(
    initialMottaker.orgnummer ? JuridiskEnhet.BEDRIFT : JuridiskEnhet.PRIVATPERSON
  )

  const [landResult, apiHentLand] = useApiCall(hentKodeverkLandISO2)

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
    if (juridiskEnhet == JuridiskEnhet.BEDRIFT) setValue('foedselsnummer', undefined)
    else if (juridiskEnhet == JuridiskEnhet.PRIVATPERSON) setValue('orgnummer', undefined)
  }, [juridiskEnhet])

  useEffect(() => {
    if (isOpen && isInitial(landResult)) {
      apiHentLand(undefined)
    }
  })

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
  const erUtenlanskAdresse = watch('adresse.adresseType') === AdresseType.UTENLANDSKPOSTADRESSE

  return (
    <>
      <Button
        variant="secondary"
        onClick={() => setIsOpen(true)}
        icon={<DocPencilIcon title="Endre mottaker" />}
        size="small"
      />
      <form onSubmit={handleSubmit(lagreEndringer)}>
        <Modal open={isOpen} onClose={avbryt} width="medium" aria-label="Endre mottaker">
          <Modal.Body>
            <VStack gap="space-4">
              <Heading size="large" spacing>
                Endre mottaker
              </Heading>

              <ToggleGroup
                defaultValue={juridiskEnhet}
                onChange={(value) => setJuridiskEnhet(value as JuridiskEnhet)}
                size="small"
              >
                <ToggleGroup.Item value={JuridiskEnhet.PRIVATPERSON}>Privatperson</ToggleGroup.Item>
                <ToggleGroup.Item value={JuridiskEnhet.BEDRIFT}>Bedrift</ToggleGroup.Item>
              </ToggleGroup>

              {juridiskEnhet == JuridiskEnhet.BEDRIFT && (
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
              {juridiskEnhet == JuridiskEnhet.PRIVATPERSON && (
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
                render={({ field: { onChange, ...rest }, formState: { errors } }) => (
                  <Select
                    {...rest}
                    label="Adressetype"
                    error={errors?.adresse?.adresseType?.message}
                    onChange={(e) => {
                      const type = e.target.value as AdresseType
                      if (type === AdresseType.NORSKPOSTADRESSE) {
                        setValue('adresse.landkode', 'NO')
                        setValue('adresse.land', 'NORGE')
                      }
                      onChange(type)
                    }}
                  >
                    <option></option>
                    <option value={AdresseType.NORSKPOSTADRESSE}>Norsk postadresse</option>
                    <option value={AdresseType.UTENLANDSKPOSTADRESSE}>Utenlandsk postadresse</option>
                  </Select>
                )}
              />

              {erUtenlanskAdresse && (
                <Alert variant="warning" size="small">
                  OBS: På utenlandske adresser må du kun bruke adresselinjer, land og landkode. Postnummer og poststed
                  er ikke gyldig
                </Alert>
              )}

              {(erNorskAdresse || erUtenlanskAdresse) && (
                <>
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

                  <HGrid columns={2} gap="space-4 space-4">
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
                </>
              )}

              {erUtenlanskAdresse &&
                mapResult(landResult, {
                  pending: <Spinner label="Henter land og landkoder" />,
                  error: (error) => <ApiErrorAlert>{error.detail}</ApiErrorAlert>,
                  success: (landkoder) => (
                    <Controller
                      name="adresse.landkode"
                      control={control}
                      render={({ field: { onChange, value } }) => {
                        const options = landkoder.map((kode) => ({
                          label: capitalize(kode.beskrivelse.term),
                          value: kode.isoLandkode,
                        }))
                        const finnTerm = (kode: string) =>
                          landkoder.find((lk) => lk.isoLandkode === kode)?.beskrivelse?.term

                        const selected = value ? finnTerm(value) : undefined

                        return (
                          <UNSAFE_Combobox
                            label="Land"
                            options={options}
                            selectedOptions={[selected ? capitalize(selected) : '']}
                            onToggleSelected={(option, isSelected) => {
                              if (isSelected) {
                                setValue('adresse.land', capitalize(finnTerm(option)))
                                onChange(option)
                              }
                            }}
                            shouldAutocomplete
                          />
                        )
                      }}
                    />
                  ),
                })}

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

              <HStack gap="space-4" justify="end">
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
