import { Alert, Button, Heading, HStack, Modal, Select, Textarea, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'
import { byttEnhetPaaSak } from '~shared/api/sak'
import { ENHETER, EnhetFilterKeys, filtrerEnhet } from '~shared/types/Enhet'
import { PencilIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useForm } from 'react-hook-form'

export const EndreEnhet = ({ sakId }: { sakId: number }) => {
  const [open, setOpen] = useState(false)
  const [endreEnhetStatus, endreEnhetKall, resetApiCall] = useApiCall(byttEnhetPaaSak)
  const [enhetViByttetTil, setEnhetViByttetTil] = useState<string>('VELGENHET')
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const harTilgangPaaNyEnhet = innloggetSaksbehandler.enheter.includes(enhetViByttetTil)

  interface EndreEnhetSkjema {
    enhet: EnhetFilterKeys
    kommentar?: string
  }

  const {
    formState: { errors },
    handleSubmit,
    register,
    reset,
    watch,
  } = useForm<EndreEnhetSkjema>({ defaultValues: { enhet: 'VELGENHET', kommentar: '' } })

  const endreEnhet = (data: EndreEnhetSkjema) => {
    console.log('Endrer til ' + data.enhet)

    endreEnhetKall({ sakId: sakId, enhet: filtrerEnhet(data.enhet) }, () => {
      setEnhetViByttetTil(filtrerEnhet(data.enhet))
    })
  }

  const closeAndReset = () => {
    console.log('Resetting')
    reset()
    resetApiCall()
  }

  return (
    <div>
      <Button
        size="small"
        variant="tertiary"
        onClick={() => setOpen(true)}
        icon={<PencilIcon aria-hidden />}
        iconPosition="right"
      >
        Endre enhet
      </Button>

      <Modal open={open} onClose={closeAndReset} aria-labelledby="modal-heading">
        <Modal.Header closeButton={false}>
          <Heading spacing level="2" size="medium" id="modal-heading">
            Endre enhet
          </Heading>
        </Modal.Header>

        <Modal.Body>
          {isSuccess(endreEnhetStatus) ? (
            <VStack gap="4">
              <Alert variant="success">Saken er flyttet til enhet {enhetViByttetTil}.</Alert>

              {!harTilgangPaaNyEnhet && (
                <Alert variant="warning">
                  Du har ikke lenger tilgang til saken, siden du ikke har tilgang til enheten saken er byttet til.
                </Alert>
              )}

              <HStack gap="2" justify="end">
                <Button variant={harTilgangPaaNyEnhet ? 'secondary' : 'primary'} as="a" href="/">
                  Gå til oppgavelisten
                </Button>
                {harTilgangPaaNyEnhet && (
                  <Button variant="primary" onClick={() => window.location.reload()}>
                    Last saken på nytt
                  </Button>
                )}
              </HStack>
            </VStack>
          ) : (
            <VStack gap="4">
              <Alert variant="warning">
                Hvis du endrer til en enhet du selv ikke har tilgang til, vil du ikke kunne flytte saken tilbake
              </Alert>

              {isFailure(endreEnhetStatus) && (
                <ApiErrorAlert>
                  Kunne ikke endre sakens enhet til {watch('enhet')} på grunn av feil: {endreEnhetStatus.error.detail}
                </ApiErrorAlert>
              )}

              <form onSubmit={handleSubmit(endreEnhet)}>
                <Select
                  {...register('enhet', {
                    required: {
                      value: true,
                      message: 'Du må velge en enhet',
                    },
                  })}
                  label="Enhet"
                  error={errors.enhet?.message}
                >
                  {Object.entries(ENHETER).map(([status, statusbeskrivelse]) => (
                    <option key={status} value={status === 'VELGENHET' ? '' : status}>
                      {statusbeskrivelse}
                    </option>
                  ))}
                </Select>

                <Textarea {...register('kommentar')} label="Kommentar" />

                {watch('enhet') !== 'VELGENHET' &&
                  !innloggetSaksbehandler.enheter.includes(filtrerEnhet(watch('enhet'))) && (
                    <Alert variant="warning">
                      Du har ikke tilgang til enhet {watch('enhet')}, og vil ikke kunne se saken etter flytting.
                    </Alert>
                  )}

                <HStack gap="2" justify="end">
                  <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
                    Avbryt
                  </Button>
                  <Button type="submit" loading={isPending(endreEnhetStatus)}>
                    Endre
                  </Button>
                </HStack>
              </form>
            </VStack>
          )}
        </Modal.Body>
      </Modal>
    </div>
  )
}
