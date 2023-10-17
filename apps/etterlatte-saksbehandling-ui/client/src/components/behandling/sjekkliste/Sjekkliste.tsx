import styled from 'styled-components'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import { SidebarPanel } from '~shared/components/Sidebar'
import { isFailure, isInitial, useApiCall } from '~shared/hooks/useApiCall'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { hentSjekkliste, oppdaterSjekkliste, oppdaterSjekklisteItem, opprettSjekkliste } from '~shared/api/sjekkliste'
import { BodyLong, Checkbox, ConfirmationPanel, Heading, Link, Textarea, TextField, VStack } from '@navikt/ds-react'
import { ConfigContext } from '~clientConfig'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { ISjekkliste, ISjekklisteItem } from '~shared/types/Sjekkliste'
import { ApiErrorAlert } from '~ErrorBoundary'
import debounce from 'lodash/debounce'

export const Sjekkliste = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const ferdigBehandlet = !hentBehandlesFraStatus(behandling.status)

  const [hentSjekklisteResult, hentSjekklisteForBehandling] = useApiCall(hentSjekkliste)
  const [opprettSjekklisteResult, opprettSjekklisteForBehandling] = useApiCall(opprettSjekkliste)
  const [oppdaterSjekklisteResult, oppdaterSjekklisteApi] = useApiCall(oppdaterSjekkliste)
  const [sjekkliste, setSjekkliste] = useState<ISjekkliste>()

  const configContext = useContext(ConfigContext)

  useEffect(() => {
    if (isInitial(hentSjekklisteResult)) {
      hentSjekklisteForBehandling(
        behandling.id,
        (result) => {
          setSjekkliste(result)
        },
        () => {
          if (!ferdigBehandlet) {
            opprettSjekklisteForBehandling(behandling.id, (opprettet) => {
              setSjekkliste(opprettet)
            })
          }
        }
      )
    }
  }, [])

  const fireOpppdater = useMemo(() => debounce(oppdaterSjekklisteApi, 1500), [])

  return (
    <SidebarPanel>
      <Heading spacing size="small">
        Sjekkliste
      </Heading>
      <BodyLong>
        Gjennomgå alle punktene og marker de som krever handling.
        <Link href="#">Her finner du rutine til punktene.</Link>
      </BodyLong>

      {isFailure(hentSjekklisteResult) && <ApiErrorAlert>En feil oppstod ved henting av sjekklista</ApiErrorAlert>}
      {isFailure(opprettSjekklisteResult) && <ApiErrorAlert>Opprettelsen av sjekkliste feilet</ApiErrorAlert>}
      {isFailure(oppdaterSjekklisteResult) && <ApiErrorAlert>Oppdateringen av sjekklista feilet</ApiErrorAlert>}

      {sjekkliste && (
        <>
          {sjekkliste?.sjekklisteItems.map((item) => (
            <SjekklisteElement
              key={item.id}
              item={item}
              behandlingId={behandling.id}
              ferdigBehandlet={ferdigBehandlet}
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
                setSjekkliste(oppdatert)
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
                setSjekkliste(oppdatert)
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
                setSjekkliste(oppdatert)
                fireOpppdater(oppdatert)
              }}
              readOnly={ferdigBehandlet}
            />

            {!ferdigBehandlet && (
              <ConfirmationPanel
                name="BekreftGjennomgang"
                checked={sjekkliste.bekreftet}
                label="Jeg bekrefter at alle punkter er gjennomgått"
                onChange={(e) => {
                  const oppdatert = {
                    ...sjekkliste,
                    bekreftet: e.currentTarget.checked,
                  }
                  setSjekkliste(oppdatert)
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
}: {
  item: ISjekklisteItem
  behandlingId: string
  ferdigBehandlet: boolean
}) => {
  const [itemUpdateResult, oppdaterItem] = useApiCall(oppdaterSjekklisteItem)

  return (
    <>
      {isFailure(itemUpdateResult) && <ApiErrorAlert>En feil oppsto ved lagring av data</ApiErrorAlert>}

      <Checkbox
        checked={item.avkrysset}
        readOnly={ferdigBehandlet}
        onChange={(event) => {
          const checked = event.currentTarget.checked
          item.avkrysset = checked
          oppdaterItem({ behandlingId, item, checked }, (oppdatert) => (item.versjon = oppdatert.versjon))
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
