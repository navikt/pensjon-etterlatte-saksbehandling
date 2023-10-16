import styled from 'styled-components'
import React, { useContext, useEffect, useState } from 'react'
import { SidebarPanel } from '~shared/components/Sidebar'
import { isFailure, isInitial, useApiCall } from '~shared/hooks/useApiCall'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { hentSjekkliste, oppdaterSjekklisteItem, opprettSjekkliste } from '~shared/api/sjekkliste'
import { BodyLong, Checkbox, Heading, Link, Textarea, TextField } from '@navikt/ds-react'
import { ConfigContext } from '~clientConfig'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { ISjekkliste, ISjekklisteItem } from '~shared/types/Sjekkliste'
import { ApiErrorAlert } from '~ErrorBoundary'

export const Sjekkliste = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const ferdigBehandlet = erFerdigBehandlet(behandling.status)
  const [hentSjekklisteResult, hentSjekklisteForBehandling] = useApiCall(hentSjekkliste)
  const [opprettSjekklisteResult, opprettSjekklisteForBehandling] = useApiCall(opprettSjekkliste)
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
  })

  return (
    <SidebarPanel>
      <Heading spacing size="small">
        Sjekkliste
      </Heading>
      <BodyLong>
        Gjennomgå alle punktene og marker de som krever handling.{' '}
        <Link href="#">Her finner du rutine til punktene.</Link>
      </BodyLong>

      {isFailure(hentSjekklisteResult) && <ApiErrorAlert>En feil oppstod ved henting av sjekklista</ApiErrorAlert>}
      {isFailure(opprettSjekklisteResult) && <ApiErrorAlert>Opprettelsen av sjekkliste feilet</ApiErrorAlert>}

      {sjekkliste && (
        <>
          {sjekkliste?.sjekklisteItems.map((item) => (
            <SjekklisteElement key={item.id} item={item} behandlingId={behandling.id} />
          ))}

          <HMargin>
            <Link
              href={`${configContext['gosysUrl']}/personoversikt/fnr=${behandling.søker!.foedselsnummer}`}
              target="_blank"
            >
              Personoversikt i Gosys
            </Link>
          </HMargin>

          <HMargin>
            <Textarea
              label="Kommentar"
              description="Skriv ned annet som ikke kommer frem i sjekklisten."
              rows={3}
              readOnly={ferdigBehandlet}
            />
          </HMargin>

          <HMargin>
            <TextField label="Adresse brevet er sendt til" readOnly={ferdigBehandlet} />
          </HMargin>

          <HMargin>
            <TextField label="Kontonummer registrert" readOnly={ferdigBehandlet} />
          </HMargin>
        </>
      )}
    </SidebarPanel>
  )
}

const SjekklisteElement = ({ item, behandlingId }: { item: ISjekklisteItem; behandlingId: string }) => {
  const [itemUpdateResult, oppdaterItem] = useApiCall(oppdaterSjekklisteItem)

  return (
    <>
      {isFailure(itemUpdateResult) && <ApiErrorAlert>En feil oppsto ved lagring av data</ApiErrorAlert>}

      <Checkbox
        checked={item.avkrysset}
        readOnly={false}
        onChange={(event) => {
          const checked = event.currentTarget.checked
          item.avkrysset = checked
          oppdaterItem({ behandlingId, item, checked }, (updatedItem) => (item.versjon = updatedItem.versjon))
        }}
      >
        {item.beskrivelse}
      </Checkbox>
    </>
  )
}

const HMargin = styled.div`
  margin-top: 1em;
`
