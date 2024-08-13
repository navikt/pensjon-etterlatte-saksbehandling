import { BodyShort, Detail, Dropdown, Heading, InternalHeader, Label } from '@navikt/ds-react'
import { BellFillIcon } from '@navikt/aksel-icons'
import { useEffect, useState } from 'react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentUtgivelser, Release } from '~shared/api/github'
import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { isToday } from 'date-fns'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'

const KEY = 'release_id_list'
const lagreLesteIder = (ider: number[]) => localStorage.setItem(KEY, JSON.stringify(ider))
const hentLesteIder = (): number[] => {
  try {
    return JSON.parse(localStorage[KEY])
  } catch {
    return []
  }
}

export const ReleaseAlerts = () => {
  const [status, hentReleases] = useApiCall(hentUtgivelser)

  const [utgivelser, setUtgivelser] = useState<Release[]>([])
  const [antallUlest, setAntallUlest] = useState(0)

  useEffect(() => {
    hentReleases({}, (utgivelser) => {
      const sett = hentLesteIder()

      const antallUleste = utgivelser.filter((u) => !sett.includes(u.id)).length
      setAntallUlest(antallUleste)

      setUtgivelser(utgivelser)
    })
  }, [])

  const open = (isOpen: boolean) => {
    if (isOpen && isSuccess(status)) {
      const lesteUtgivelser = utgivelser.map((u) => u.id)
      lagreLesteIder(lesteUtgivelser)
      setAntallUlest(0)
    }
  }

  return (
    <Dropdown onOpenChange={(isOpen) => open(isOpen)}>
      <InternalHeader.Button as={Dropdown.Toggle}>
        <div style={{ position: 'relative' }}>
          <BellFillIcon style={{ fontSize: '1.5rem' }} />
          {antallUlest > 0 && <UnreadCircle>{antallUlest}</UnreadCircle>}
        </div>
      </InternalHeader.Button>

      <DropdownMenu>
        {mapApiResult(
          status,
          <Spinner label="Henter siste utgivelser" />,
          () => (
            <ApiErrorAlert>Kunne ikke hente siste utgivelser</ApiErrorAlert>
          ),
          () => (
            <>
              <Heading size="small">Hva er nytt i Gjenny?</Heading>

              {utgivelser.map((utgivelse, i) => (
                <UtgivelseInfo key={`utgivelse-${i}`}>
                  <Label size="small" as="p">
                    {utgivelse.name}
                  </Label>
                  <Detail spacing>
                    {formaterDatoMedKlokkeslett(utgivelse.published_at)}{' '}
                    {isToday(new Date(utgivelse.published_at)) && '(i dag)'}
                  </Detail>
                  <UtgivelseBody size="small" spacing>
                    {utgivelse.body}
                  </UtgivelseBody>
                </UtgivelseInfo>
              ))}
            </>
          )
        )}
      </DropdownMenu>
    </Dropdown>
  )
}

const DropdownMenu = styled(Dropdown.Menu)`
  min-width: fit-content;
  max-width: fit-content;
`

const UnreadCircle = styled.div`
  width: 15px;
  height: 15px;
  padding: 10px;
  background: var(--nav-error-border);
  color: white;
  font-size: 15px;
  border-radius: 100%;

  display: inline-flex;
  justify-content: center;
  align-items: center;

  position: absolute;
  top: -10px;
  right: -10px;
`

const UtgivelseInfo = styled.div`
  width: 25rem;

  :not(:last-child) {
    border-bottom: 1px solid gray;
  }
`

const UtgivelseBody = styled(BodyShort)`
  white-space: pre-wrap;
`
