import { Alert, Button, Heading, HStack, Modal, Select, Textarea, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'
import { byttEnhetPaaSak } from '~shared/api/sak'
import { ENHETER } from '~shared/types/Enhet'
import { PencilIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useForm } from 'react-hook-form'
import { ClickEvent, trackClick } from '~utils/amplitude'

interface EndreEnhetSkjema {
  enhet: string
  kommentar?: string
}

export const EndreEnhet = ({ sakId, gjeldendeEnhet }: { sakId: number; gjeldendeEnhet: string }) => {
  const [open, setOpen] = useState(false)
  const [endreEnhetStatus, endreEnhetKall, resetApiCall] = useApiCall(byttEnhetPaaSak)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const {
    formState: { errors },
    handleSubmit,
    register,
    reset,
    watch,
  } = useForm<EndreEnhetSkjema>({ defaultValues: { enhet: gjeldendeEnhet, kommentar: '' } })

  const valgtEnhet = watch('enhet')
  const harTilgangPaaNyEnhet = innloggetSaksbehandler.enheter.includes(valgtEnhet ?? '')

  const endreEnhet = (data: EndreEnhetSkjema) => {
    endreEnhetKall({ sakId: sakId, enhet: data.enhet, kommentar: data.kommentar }, () => {
      trackClick(ClickEvent.MANUELT_ENDRET_ENHET)
    })
  }

  const closeAndReset = () => {
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
        Endre
      </Button>

      <Modal open={open} onClose={closeAndReset} width="40rem" aria-labelledby="modal-heading">
        <Modal.Header closeButton={false}>
          <Heading spacing level="2" size="medium" id="modal-heading">
            Endre enhet
          </Heading>
        </Modal.Header>

        <Modal.Body>
          {isSuccess(endreEnhetStatus) ? (
            <VStack gap="4">
              <Alert variant="success">Saken er flyttet til enhet &quot;{ENHETER[valgtEnhet]}&quot;.</Alert>

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
              <form onSubmit={handleSubmit(endreEnhet)}>
                <VStack gap="5">
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
                    {Object.entries(ENHETER).map(([id, navn]) => (
                      <option key={id} value={id}>
                        {navn}
                      </option>
                    ))}
                  </Select>

                  {valgtEnhet !== gjeldendeEnhet && !innloggetSaksbehandler.enheter.includes(valgtEnhet) && (
                    <Alert variant="warning">
                      Du har ikke tilgang til &quot;{ENHETER[valgtEnhet]}&quot;, og vil ikke kunne se saken etter
                      flytting.
                    </Alert>
                  )}

                  <Textarea
                    {...register('kommentar', {
                      required: {
                        value: true,
                        message: 'Du må begrunne hvorfor enhet endres',
                      },
                    })}
                    error={errors.kommentar?.message}
                    label="Kommentar"
                  />

                  <HStack gap="2" justify="end">
                    {isFailure(endreEnhetStatus) && (
                      <ApiErrorAlert>
                        Kunne ikke endre sakens enhet til &quot;{valgtEnhet}&quot; på grunn av feil:{' '}
                        {endreEnhetStatus.error.detail}
                      </ApiErrorAlert>
                    )}

                    <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
                      Avbryt
                    </Button>
                    <Button type="submit" loading={isPending(endreEnhetStatus)}>
                      Endre
                    </Button>
                  </HStack>
                </VStack>
              </form>
            </VStack>
          )}
        </Modal.Body>
      </Modal>
    </div>
  )
}
