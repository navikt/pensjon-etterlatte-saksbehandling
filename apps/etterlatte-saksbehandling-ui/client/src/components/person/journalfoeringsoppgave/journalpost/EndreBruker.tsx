import { Bruker, BrukerIdType } from '~shared/types/Journalpost'
import React, { useEffect, useState } from 'react'
import { Alert, BodyShort, Box, Button, CopyButton, Heading, HStack, Label, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonNavnogFoedsel } from '~shared/api/pdltjenester'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { mapResult, mapSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { formaterNavn } from '~shared/types/Person'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { PersonSoekModal } from '~components/person/journalfoeringsoppgave/journalpost/modal/PersonSoekModal'

const formaterType = (type: BrukerIdType) => {
  switch (type) {
    case BrukerIdType.ORGNR:
      return 'Orgnr.'
    case BrukerIdType.AKTOERID:
      return 'Aktørid'
    case BrukerIdType.FNR:
      return 'Fødselsnummer'
  }
}

export const EndreBruker = ({
  bruker,
  oppdaterBruker,
}: {
  bruker?: Bruker
  oppdaterBruker: (bruker: Bruker) => void
}) => {
  const [initieltFnr, setInitieltFnr] = useState<string>()
  const [personResult, hentPerson, resetPerson] = useApiCall(hentPersonNavnogFoedsel)
  const kanRedigereBruker = useFeatureEnabledMedDefault('kan-redigere-journalpost-bruker', false)

  const {
    register,
    formState: { errors },
    handleSubmit,
    watch,
    reset,
    setValue,
  } = useForm<Bruker>({ defaultValues: bruker })

  const [rediger, setRediger] = useState(false)

  const lagreEndring = (bruker: Bruker) => {
    oppdaterBruker({ ...bruker, type: BrukerIdType.FNR })
    setRediger(false)
  }

  const avbryt = () => {
    reset({ ...bruker })
    resetPerson()
    setRediger(false)
  }

  useEffect(() => {
    const id = watch((bruker) => {
      if (fnrHarGyldigFormat(bruker?.id)) {
        hentPerson(bruker.id!!)
      }
    })

    return () => id.unsubscribe()
  }, [watch])

  useEffect(() => {
    if (bruker?.id) {
      hentPerson(bruker.id, (result) => {
        setInitieltFnr(result.foedselsnummer)
      })
    }
  }, [bruker])

  return (
    <div>
      <Heading size="small" spacing>
        Bruker
      </Heading>

      {rediger ? (
        <Box background="bg-subtle" padding="4" borderColor="border-subtle" borderWidth="1" borderRadius="medium">
          <VStack gap="8">
            <VStack gap="4">
              <TextField
                {...register('id', {
                  pattern: {
                    value: /[0-9]{11}/,
                    message: 'Fødselsnummer må bestå av 11 siffer',
                  },
                })}
                label="Fødselsnummer"
                error={errors?.id?.message}
                htmlSize={50}
              />

              {mapResult(personResult, {
                pending: <Spinner visible label="Søker etter person..." />,
                success: (person) => (
                  <>
                    <Alert variant="info" size="small">
                      {formaterNavn(person)}
                    </Alert>
                    {!!initieltFnr && person.foedselsnummer !== initieltFnr && (
                      <Alert variant="warning" size="small">
                        Du flytter nå journalposten til en annen bruker
                      </Alert>
                    )}
                  </>
                ),
              })}
            </VStack>

            <VStack gap="4">
              <Label>Har du ikke fødselsnummeret?</Label>
              <PersonSoekModal
                velgPerson={({ id }) => {
                  setValue('id', id, { shouldDirty: true, shouldValidate: true })
                  setValue('type', BrukerIdType.FNR)
                }}
              />
            </VStack>
          </VStack>

          <HStack gap="4" justify="end">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>

            <Button onClick={handleSubmit(lagreEndring)} size="small">
              Lagre
            </Button>
          </HStack>
        </Box>
      ) : (
        <VStack gap="4">
          <HStack gap="4" justify="space-between" align="center">
            <BodyShort as="div" spacing>
              {mapSuccess(personResult, (person) => formaterNavn(person))}

              {bruker?.id && (
                <CopyButton
                  copyText={bruker.id}
                  size="small"
                  text={
                    bruker.id +
                    (!!bruker?.type && bruker.type !== BrukerIdType.FNR ? ` (${formaterType(bruker.type)})` : '')
                  }
                />
              )}
            </BodyShort>

            {kanRedigereBruker && (
              <Button variant="secondary" size="small" onClick={() => setRediger(true)}>
                Endre
              </Button>
            )}
          </HStack>

          {!bruker?.id && <Alert variant="warning">Bruker må være satt</Alert>}
        </VStack>
      )}
    </div>
  )
}
