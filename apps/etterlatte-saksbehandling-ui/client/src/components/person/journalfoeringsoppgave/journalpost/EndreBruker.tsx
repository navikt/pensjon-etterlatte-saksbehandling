import { AvsenderMottaker, Bruker, BrukerIdType } from '~shared/types/Journalpost'
import React, { useEffect, useState } from 'react'
import { Alert, BodyShort, Button, CopyButton, Heading, HStack, TextField } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { InputFlexRow } from '~components/person/journalfoeringsoppgave/journalpost/OppdaterJournalpost'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonNavnogFoedsel } from '~shared/api/pdltjenester'
import { fnrHarGyldigFormat } from '~utils/fnr'
import { mapResult, mapSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { formaterNavn } from '~shared/types/Person'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import SoekPersonPdl from '~components/person/journalfoeringsoppgave/journalpost/modal/SoekPersonPdl'

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
  } = useForm<AvsenderMottaker>({ defaultValues: bruker })

  const [rediger, setRediger] = useState(false)
  const [open, setOpen] = useState(false)

  const lagreEndretMottaker = (bruker: Bruker) => {
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
        <>
          <TextField
            {...register('id', {
              pattern: {
                value: /[0-9]{11}/,
                message: 'Fødselsnummer må bestå av 11 siffer',
              },
            })}
            label="Fødselsnummer"
            error={errors?.id?.message}
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
          <br />
          <SoekPersonPdl open={open} setOpen={setOpen} />

          <HStack gap="4" justify="end">
            <Button variant="tertiary" onClick={avbryt} size="small">
              Avbryt
            </Button>
            <Button variant="secondary" onClick={handleSubmit(lagreEndretMottaker)} size="small">
              Lagre
            </Button>
            <Button onClick={() => setOpen(!open)}>Avansert Søk</Button>
          </HStack>
        </>
      ) : (
        <>
          <InputFlexRow>
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
          </InputFlexRow>

          <br />

          {!bruker?.id && <Alert variant="warning">Bruker må være satt</Alert>}
        </>
      )}
    </div>
  )
}
