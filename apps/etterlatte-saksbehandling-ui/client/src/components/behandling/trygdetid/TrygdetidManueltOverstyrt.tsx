import React, { useState } from 'react'
import { Alert, BodyShort, Box, Button, Checkbox, Heading, HStack, TextField, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  IDetaljertBeregnetTrygdetid,
  ITrygdetid,
  oppdaterTrygdetidOverstyrtMigrering,
  opprettTrygdetidOverstyrtMigrering,
} from '~shared/api/trygdetid'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Toast } from '~shared/alerts/Toast'
import { useBehandling } from '~components/behandling/useBehandling'

export const TrygdetidManueltOverstyrt = ({
  trygdetidId,
  ident,
  beregnetTrygdetid,
  oppdaterTrygdetid,
  tidligereFamiliepleier,
  redigerbar,
}: {
  trygdetidId: string
  ident: string
  beregnetTrygdetid: IDetaljertBeregnetTrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  tidligereFamiliepleier?: boolean
  redigerbar: boolean
}) => {
  const personopplysninger = usePersonopplysninger()
  const behandling = useBehandling()

  const [skalHaProrata, setSkalHaProrata] = useState<boolean>(beregnetTrygdetid.resultat.prorataBroek != null)

  const [anvendtTrygdetid, setAnvendtTrygdetid] = useState<number | undefined>(
    skalHaProrata
      ? beregnetTrygdetid.resultat.samletTrygdetidTeoretisk
      : beregnetTrygdetid.resultat.samletTrygdetidNorge
  )

  const [prorataTeller, setTeller] = useState<number | undefined>(beregnetTrygdetid.resultat.prorataBroek?.teller)
  const [prorataNevner, setNevner] = useState<number | undefined>(beregnetTrygdetid.resultat.prorataBroek?.nevner)

  const [oppdaterStatus, oppdaterTrygdetidRequest] = useApiCall(oppdaterTrygdetidOverstyrtMigrering)
  const [opprettStatus, opprettOverstyrtTrygdetid] = useApiCall(opprettTrygdetidOverstyrtMigrering)

  const lagre = () => {
    oppdaterTrygdetidRequest(
      {
        behandlingId: behandling!!.id,
        trygdetidId: trygdetidId,
        anvendtTrygdetid: anvendtTrygdetid!!,
        prorataBroek: skalHaProrata
          ? {
              teller: prorataTeller!!,
              nevner: prorataNevner!!,
            }
          : undefined,
      },
      (trygdetid) => {
        oppdaterTrygdetid(trygdetid)
      }
    )
  }

  const overskrivOverstyrtTrygdetid = () => {
    opprettOverstyrtTrygdetid({ behandlingId: behandling!!.id, overskriv: true }, () => window.location.reload())
  }

  if (!behandling) return <ApiErrorAlert>Fant ikke behandling</ApiErrorAlert>

  const identErIGrunnlag = personopplysninger?.avdoede?.find((person) => person.opplysning.foedselsnummer === ident)
  if (!identErIGrunnlag && !tidligereFamiliepleier) {
    if (ident !== 'UKJENT_AVDOED') {
      return <Alert variant="error">Fant ikke avdød ident {ident} (trygdetid) i behandlingsgrunnlaget</Alert>
    }
  }

  return (
    <>
      <Heading size="small" level="3">
        Manuelt overstyrt trygdetid
      </Heading>

      {redigerbar && (
        <>
          {ident == 'UKJENT_AVDOED' && (
            <Box maxWidth="40rem">
              <VStack gap="1">
                <Alert variant="warning">
                  OBS! Trygdetiden er koblet til ukjent avdød. Hvis avdød i saken faktisk er kjent bør du annullere
                  behandlingen og lage en ny behandling med riktig avdød i familieoversikten.
                </Alert>
                <Box maxWidth="20rem">
                  <Button
                    variant="danger"
                    size="small"
                    onClick={overskrivOverstyrtTrygdetid}
                    loading={isPending(opprettStatus)}
                  >
                    Opprett overstyrt trygdetid på nytt
                  </Button>
                </Box>
              </VStack>
            </Box>
          )}
          <VStack gap="4">
            <TextField
              label="Anvendt trygdetid"
              placeholder="Anvendt trygdetid"
              value={anvendtTrygdetid || ''}
              pattern="[0-9]{11}"
              maxLength={11}
              htmlSize={20}
              onChange={(e) => setAnvendtTrygdetid(Number(e.target.value))}
            />

            <Checkbox checked={skalHaProrata} onChange={() => setSkalHaProrata(!skalHaProrata)}>
              Prorata brøk
            </Checkbox>
            {skalHaProrata && (
              <HStack gap="4">
                <Box width="20rem">
                  <TextField
                    label="Prorata teller"
                    placeholder="Prorata teller"
                    value={prorataTeller || ''}
                    pattern="[0-9]{11}"
                    maxLength={11}
                    onChange={(e) => setTeller(Number(e.target.value))}
                  />
                </Box>
                <Box width="20rem">
                  <TextField
                    label="Prorata nevner"
                    placeholder="Prorata nevner"
                    value={prorataNevner || ''}
                    pattern="[0-9]{11}"
                    maxLength={11}
                    onChange={(e) => setNevner(Number(e.target.value))}
                  />
                </Box>
              </HStack>
            )}

            <Button
              variant="secondary"
              size="small"
              onClick={lagre}
              loading={isPending(oppdaterStatus)}
              disabled={anvendtTrygdetid == null || (skalHaProrata && (prorataNevner == null || prorataTeller == null))}
            >
                Lagre overstyrt trygdetid
            </Button>
          </VStack>
          {mapResult(oppdaterStatus, {
            pending: <Spinner label="Lagrer trygdetid" />,
            error: () => <ApiErrorAlert>En feil har oppstått ved lagring av trygdetid</ApiErrorAlert>,
            success: () => <Toast melding="Trygdetid lagret" position="bottom-center" />,
          })}
          {mapResult(opprettStatus, {
            pending: <Spinner label="Overstyrer trygdetid" />,
            error: () => <ApiErrorAlert>En feil har oppstått ved overstyring av trygdetid</ApiErrorAlert>,
            success: () => <Toast melding="Trygdetid overstyrt" position="bottom-center" />,
          })}
        </>
      )}
      {!redigerbar && (
        <VStack gap="2">
          {ident === 'UKJENT_AVDOED' && <Alert variant="warning">OBS! Trygdetiden er koblet til ukjent avdød.</Alert>}
          <Heading size="xsmall" level="4">
            Anvendt trygdetid
          </Heading>
          <BodyShort>{anvendtTrygdetid}</BodyShort>
          <Heading size="xsmall" level="4">
            Prorata-brøk
          </Heading>
          <BodyShort>{skalHaProrata ? 'Ja' : 'Nei'}</BodyShort>

          <Checkbox checked={skalHaProrata} readOnly={true}>
            Prorata-brøk
          </Checkbox>
          {skalHaProrata && (
            <HStack gap="4">
              <Box>
                <Heading size="xsmall" level="4">
                  Prorata teller
                </Heading>
                <BodyShort>{prorataTeller}</BodyShort>
              </Box>
              <Box>
                <Heading size="xsmall" level="4">
                  Prorata nevner
                </Heading>
                <BodyShort>{prorataNevner}</BodyShort>
              </Box>
            </HStack>
          )}
        </VStack>
      )}
    </>
  )
}
