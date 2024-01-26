import { Button, Heading, Modal, Select, TextField, ToggleGroup } from '@navikt/ds-react'
import React, { Dispatch, SetStateAction, useEffect, useState } from 'react'
import { AdresseType, IBrev, Mottaker } from '~shared/types/Brev'
import styled, { css } from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterMottaker } from '~shared/api/brev'
import { FlexRow } from '~shared/styled'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { KildePersondata } from '~shared/types/kilde'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { formaterFnr } from '~utils/formattering'
import { Controller, useForm } from 'react-hook-form'

enum MottakerType {
  PRIVATPERSON = 'PRIVATPERSON',
  BEDRIFT = 'BEDRIFT',
}

interface Props {
  brev: IBrev
  setBrev: Dispatch<SetStateAction<IBrev>>
  vergeadresse: Grunnlagsopplysning<Mottaker, KildePersondata> | undefined
  isOpen: boolean
  setIsOpen: Dispatch<SetStateAction<boolean>>
}

export function BrevMottakerModal({ brev, setBrev, vergeadresse, isOpen, setIsOpen }: Props) {
  const { id: brevId, sakId, mottaker: initialMottaker } = brev

  const [mottakerStatus, apiOppdaterMottaker] = useApiCall(oppdaterMottaker)

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

  function formaterAdresse(vergeadresse: Mottaker) {
    return (
      <>
        {!vergeadresse.foedselsnummer && !vergeadresse.orgnummer && `Fødselsnummer/orgnummer: Ikke registrert.`}
        {!vergeadresse.foedselsnummer && !vergeadresse.orgnummer && <br />}
        {vergeadresse.foedselsnummer && `Fødselsnummer: ${formaterFnr(vergeadresse.foedselsnummer.value)}`}
        {vergeadresse.foedselsnummer && <br />}
        {vergeadresse.navn}
        {vergeadresse.navn && <br />}
        {vergeadresse.adresse.adresselinje1}
        {vergeadresse.adresse.adresselinje1 && <br />}
        {vergeadresse.adresse.adresselinje2}
        {vergeadresse.adresse.adresselinje2 && <br />}
        {vergeadresse.adresse.adresselinje3}
        {vergeadresse.adresse.adresselinje3 && <br />}
        {vergeadresse.adresse.postnummer} {vergeadresse.adresse.poststed} {vergeadresse.adresse.land} (
        {vergeadresse.adresse.landkode})
      </>
    )
  }

  const erNorskAdresse = watch('adresse.adresseType') === AdresseType.NORSKPOSTADRESSE

  return (
    <>
      <form onSubmit={handleSubmit((data) => lagre(data))}>
        <MottakerModal open={isOpen} onClose={avbryt}>
          <Modal.Body>
            <Heading size="large" spacing>
              Endre mottaker
            </Heading>

            {vergeadresse && (
              <InfoWrapper>
                <Info wide label="Verges adresse" tekst={formaterAdresse(vergeadresse.opplysning)} />
                <br />
              </InfoWrapper>
            )}

            <SkjemaGruppe>
              <ToggleGroup
                defaultValue={mottakerType}
                onChange={(value) => setMottakerType(value as MottakerType)}
                size="small"
                style={{ marginBottom: '1rem' }}
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
                      value: /[0-9]+/,
                      message: 'Orgnummer kan kun bestå av siffer',
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
                      value: /[0-9]{11}/,
                      message: 'Fødselsnummer kan kun bestå av 11 siffer',
                    },
                  })}
                  label="Fødselsnummer"
                  error={errors?.foedselsnummer?.value?.message}
                />
              )}
            </SkjemaGruppe>

            <SkjemaGruppe>
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
            </SkjemaGruppe>

            <SkjemaGruppe>
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
            </SkjemaGruppe>

            <SkjemaGruppe>
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
            </SkjemaGruppe>

            <SkjemaGruppe inline>
              <TextField
                {...register('adresse.postnummer', {
                  required: {
                    value: erNorskAdresse,
                    message: 'Postnummer må være satt på norske adresser',
                  },
                  pattern: {
                    value: /\d{4}/,
                    message: 'Postnummer skal være fire siffer',
                  },
                })}
                label="Postnummer"
                error={errors?.adresse?.postnummer?.message}
              />
              <TextField
                {...register('adresse.poststed', {
                  required: {
                    value: erNorskAdresse,
                    message: 'Poststed må være satt',
                  },
                })}
                label="Poststed"
                error={errors?.adresse?.poststed?.message}
              />
            </SkjemaGruppe>

            <SkjemaGruppe inline>
              <TextField
                {...register('adresse.landkode', {
                  required: {
                    value: true,
                    message: 'Landkode må være satt (2 tegn)',
                  },
                  pattern: {
                    value: /[A-Z]{2}/,
                    message: 'Landkode skal kun bestå av 2 bokstaver (eks. NO)',
                  },
                })}
                label="Landkode"
                error={errors?.adresse?.landkode?.message}
              />
              <TextField
                {...register('adresse.land', {
                  required: {
                    value: true,
                    message: 'Land må være satt (2 tegn)',
                  },
                })}
                label="Land"
                error={errors?.adresse?.land?.message}
              />
            </SkjemaGruppe>

            {isFailureHandler({
              apiResult: mottakerStatus,
              errorMessage: 'Kunne ikke oppdatere mottaker.',
            })}

            <FlexRow justify="right">
              <Button variant="secondary" disabled={isPending(mottakerStatus)} onClick={avbryt}>
                Avbryt
              </Button>
              <Button variant="primary" type="submit" loading={isPending(mottakerStatus)}>
                Lagre
              </Button>
            </FlexRow>
          </Modal.Body>
        </MottakerModal>
      </form>
    </>
  )
}

const MottakerModal = styled(Modal)`
  width: 40rem;
  padding: 3rem;
`

const SkjemaGruppe = styled.div<{ inline?: boolean }>`
  & > * {
    margin-bottom: 1rem;
  }

  ${(props) => {
    if (props.inline)
      return css`
        display: flex;
        flex-direction: row;
        gap: 1rem;

        & > * {
          flex: 1;
        }
      `
  }};
`
