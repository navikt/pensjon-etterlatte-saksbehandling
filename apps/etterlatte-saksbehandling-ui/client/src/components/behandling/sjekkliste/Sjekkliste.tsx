import styled from 'styled-components'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import { SidebarPanel } from '~shared/components/Sidebar'
import { isFailure, isInitial, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { hentSjekkliste, oppdaterSjekkliste, oppdaterSjekklisteItem, opprettSjekkliste } from '~shared/api/sjekkliste'
import {
  Alert,
  BodyLong,
  Checkbox,
  ConfirmationPanel,
  Heading,
  Link,
  Textarea,
  TextField,
  VStack,
} from '@navikt/ds-react'
import { ConfigContext } from '~clientConfig'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { ISjekklisteItem } from '~shared/types/Sjekkliste'
import { ApiErrorAlert } from '~ErrorBoundary'
import debounce from 'lodash/debounce'
import { useSjekkliste, useSjekklisteValideringsfeil } from '~components/behandling/sjekkliste/useSjekkliste'
import { useAppDispatch } from '~store/Store'
import { resetValideringsfeil, updateSjekkliste, updateSjekklisteItem } from '~store/reducers/SjekklisteReducer'
import Spinner from '~shared/Spinner'

export const Sjekkliste = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const ferdigBehandlet = !hentBehandlesFraStatus(behandling.status)

  const dispatch = useAppDispatch()

  const [hentSjekklisteResult, hentSjekklisteForBehandling] = useApiCall(hentSjekkliste)
  const [opprettSjekklisteResult, opprettSjekklisteForBehandling] = useApiCall(opprettSjekkliste)
  const [oppdaterSjekklisteResult, oppdaterSjekklisteApi] = useApiCall(oppdaterSjekkliste)
  const sjekkliste = useSjekkliste()
  const sjekklisteValideringsfeil = useSjekklisteValideringsfeil().length > 0

  const configContext = useContext(ConfigContext)

  const dispatchUpdatedItem = (item: ISjekklisteItem) => {
    dispatch(updateSjekklisteItem(item))
  }

  useEffect(() => {
    if (isInitial(hentSjekklisteResult)) {
      hentSjekklisteForBehandling(
        behandling.id,
        (result) => {
          dispatch(updateSjekkliste(result))
        },
        () => {
          if (!ferdigBehandlet) {
            opprettSjekklisteForBehandling(behandling.id, (opprettet) => {
              dispatch(updateSjekkliste(opprettet))
            })
          }
        }
      )
    }
  }, [])

  useEffect(() => {
    dispatch(resetValideringsfeil())
  }, [sjekkliste])

  const fireOpppdater = useMemo(() => debounce(oppdaterSjekklisteApi, 1500), [])

  return (
    <SidebarPanel border>
      {sjekklisteValideringsfeil && (
        <Alert variant="error">Før du kan sende til attestering må du bekrefte at alle punkter er gjennomgått</Alert>
      )}

      <Heading spacing size="small">
        Sjekkliste
      </Heading>

      {isPending(hentSjekklisteResult) && <Spinner label="Henter sjekkliste" visible />}

      {isFailure(hentSjekklisteResult) && <ApiErrorAlert>En feil oppstod ved henting av sjekklista</ApiErrorAlert>}
      {isFailure(opprettSjekklisteResult) && <ApiErrorAlert>Opprettelsen av sjekkliste feilet</ApiErrorAlert>}
      {isFailure(oppdaterSjekklisteResult) && <ApiErrorAlert>Oppdateringen av sjekklista feilet</ApiErrorAlert>}

      {sjekkliste && (
        <>
          <BodyLong>
            Gjennomgå alle punktene og marker de som krever handling.
            <Link href="#">Her finner du rutine til punktene.</Link>
          </BodyLong>

          {sjekkliste?.sjekklisteItems.map((item) => (
            <SjekklisteElement
              key={item.id}
              item={item}
              behandlingId={behandling.id}
              ferdigBehandlet={ferdigBehandlet}
              onUpdated={dispatchUpdatedItem}
            />
          ))}

          <HMargin>
            <Link
              href={`${configContext['gosysUrl']}/personoversikt/fnr=${behandling.søker!.foedselsnummer}`}
              target="_blank"
            >
              Personoversikt i Gosys
            </Link>
          </HMargin>

          <VStack gap="4">
            <Textarea
              label="Kommentar"
              name="Kommentar"
              description="Skriv ned annet som ikke kommer frem i sjekklisten."
              value={sjekkliste.kommentar || ''}
              onChange={(e) => {
                const oppdatert = {
                  ...sjekkliste,
                  kommentar: e.currentTarget.value,
                }
                dispatch(updateSjekkliste(oppdatert))
                fireOpppdater(oppdatert)
              }}
              rows={3}
              readOnly={ferdigBehandlet}
            />

            <TextField
              label="Adresse brevet er sendt til"
              name="AdresseBrevforsendelse"
              value={sjekkliste.adresseForBrev || ''}
              onChange={(e) => {
                const oppdatert = {
                  ...sjekkliste,
                  adresseForBrev: e.currentTarget.value,
                }
                dispatch(updateSjekkliste(oppdatert))
                fireOpppdater(oppdatert)
              }}
              readOnly={ferdigBehandlet}
            />

            <TextField
              label="Kontonummer registrert"
              name="KontonummerRegistrert"
              value={sjekkliste.kontonrRegistrert || ''}
              onChange={(e) => {
                const oppdatert = {
                  ...sjekkliste,
                  kontonrRegistrert: e.currentTarget.value,
                }
                dispatch(updateSjekkliste(oppdatert))
                fireOpppdater(oppdatert)
              }}
              readOnly={ferdigBehandlet}
            />

            {!ferdigBehandlet && (
              <ConfirmationPanel
                name="BekreftGjennomgang"
                checked={sjekkliste.bekreftet}
                label="Jeg bekrefter at alle punkter er gjennomgått"
                error={sjekklisteValideringsfeil && !sjekkliste.bekreftet && 'Feltet må hukes av for å ferdigstilles'}
                onChange={(e) => {
                  const oppdatert = {
                    ...sjekkliste,
                    bekreftet: e.currentTarget.checked,
                  }
                  dispatch(updateSjekkliste(oppdatert))
                  fireOpppdater(oppdatert)
                }}
              />
            )}
          </VStack>
        </>
      )}
    </SidebarPanel>
  )
}

const SjekklisteElement = ({
  item,
  behandlingId,
  ferdigBehandlet,
  onUpdated,
}: {
  item: ISjekklisteItem
  behandlingId: string
  ferdigBehandlet: boolean
  onUpdated: (item: ISjekklisteItem) => void
}) => {
  const [avkrysset, setAvkrysset] = useState<boolean>(item.avkrysset)
  const [itemUpdateResult, oppdaterItem] = useApiCall(oppdaterSjekklisteItem)

  return (
    <>
      {isFailure(itemUpdateResult) && <ApiErrorAlert>En feil oppsto ved oppdatering av sjekklista</ApiErrorAlert>}

      <Checkbox
        checked={avkrysset}
        readOnly={ferdigBehandlet}
        onChange={(event) => {
          const checked = event.currentTarget.checked
          setAvkrysset(checked)
          oppdaterItem({ behandlingId, item, checked }, (oppdatert) => onUpdated(oppdatert))
        }}
      >
        {item.beskrivelse}
      </Checkbox>
    </>
  )
}

const HMargin = styled.div`
  margin-top: 1em;
  margin-bottom: 1em;
`
