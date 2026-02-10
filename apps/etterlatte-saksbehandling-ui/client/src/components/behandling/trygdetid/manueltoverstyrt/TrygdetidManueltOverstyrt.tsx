import React, { useState } from 'react'
import { Alert, BodyLong, Box, Button, Heading, VStack } from '@navikt/ds-react'
import { IDetaljertBeregnetTrygdetid, ITrygdetid, opprettTrygdetider } from '~shared/api/trygdetid'
import { TrygdetidManueltOverstyrtVisning } from '~components/behandling/trygdetid/manueltoverstyrt/TrygdetidManueltOverstyrtVisning'
import { TrygdetidManueltOverstyrtSkjema } from '~components/behandling/trygdetid/manueltoverstyrt/TrygdetidManueltOverstyrtSkjema'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useBehandling } from '~components/behandling/useBehandling'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Toast } from '~shared/alerts/Toast'

export const TrygdetidManueltOverstyrt = ({
  trygdetidId,
  ident,
  beregnetTrygdetid,
  oppdaterTrygdetid,
  redigerbar,
}: {
  trygdetidId: string
  ident: string
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  tidligereFamiliepleier?: boolean
  redigerbar: boolean
}) => {
  const trygdetidErSatt = !!beregnetTrygdetid.resultat.prorataBroek
    ? beregnetTrygdetid.resultat.samletTrygdetidTeoretisk
    : beregnetTrygdetid.resultat.samletTrygdetidNorge
  const [visSkjema, setVisSkjema] = useState(!trygdetidErSatt && redigerbar)

  return (
    <VStack gap="space-4">
      <Heading size="small" level="3">
        Manuelt overstyrt trygdetid
      </Heading>

      {visSkjema ? (
        <TrygdetidManueltOverstyrtSkjema
          trygdetidId={trygdetidId}
          beregnetTrygdetid={beregnetTrygdetid}
          oppdaterTrygdetid={oppdaterTrygdetid}
          setVisSkjema={setVisSkjema}
        />
      ) : (
        <TrygdetidManueltOverstyrtVisning
          beregnetTrygdetid={beregnetTrygdetid}
          setVisSkjema={setVisSkjema}
          redigerbar={redigerbar}
        />
      )}

      {redigerbar && <TrygdetidUkjentAvdoed ident={ident} />}
    </VStack>
  )
}

const TrygdetidUkjentAvdoed = ({
  ident,
  tidligereFamiliepleier,
}: {
  ident: string
  tidligereFamiliepleier?: boolean
}) => {
  const personopplysninger = usePersonopplysninger()
  const behandling = useBehandling()
  const [opprettStatus, opprettTrygdetid] = useApiCall(opprettTrygdetider)

  const identErIGrunnlag = personopplysninger?.avdoede?.find((person) => person.opplysning.foedselsnummer === ident)

  const opprettNyTrygdetid = () => {
    opprettTrygdetid({ behandlingId: behandling!!.id, overskriv: true }, () => window.location.reload())
  }

  return (
    <>
      {ident == 'UKJENT_AVDOED' ? (
        <Box maxWidth="40rem" marginBlock="space-8 space-0">
          <VStack gap="space-2">
            <Alert variant="warning">
              <VStack gap="space-4">
                <BodyLong>
                  Trygdetiden er koblet til en ukjent avdød. Hvis avdøde i saken er kjent, og familieoversikten er
                  oppdatert, bør trygdetid opprettes på nytt. Dette for å unngå å bruke manuelt overstyrt trygdetid der
                  dette ikke er nødvendig.
                </BodyLong>
                <VStack gap="space-4">
                  {mapResult(opprettStatus, {
                    pending: <Spinner label="Oppretter trygdetid" />,
                    error: () => <ApiErrorAlert>En feil har oppstått ved opprettelse av trygdetid</ApiErrorAlert>,
                    success: () => <Toast melding="Trygdetid opprettet" position="bottom-center" />,
                  })}
                  <Box>
                    <Button
                      variant="primary"
                      size="small"
                      onClick={opprettNyTrygdetid}
                      loading={isPending(opprettStatus)}
                    >
                      Opprett ny trygdetid
                    </Button>
                  </Box>
                </VStack>
              </VStack>
            </Alert>
          </VStack>
        </Box>
      ) : (
        !identErIGrunnlag &&
        !tidligereFamiliepleier && <Alert variant="error">Fant ikke avdød ident {ident} i behandlingsgrunnlaget</Alert>
      )}
    </>
  )
}
