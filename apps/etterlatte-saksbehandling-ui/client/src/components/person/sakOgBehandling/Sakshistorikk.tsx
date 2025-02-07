import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapResult } from '~shared/api/apiUtils'
import { hentSaksendringer } from '~shared/api/sak'
import Spinner from '~shared/Spinner'
import { BodyShort, Box, Detail, List } from '@navikt/ds-react'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { Endringstype, Identtype, ISaksendring, tekstEndringstype } from '~shared/types/sak'
import { tekstEnhet } from '~shared/types/Enhet'
import { BagdeIcon, Buildings3Icon, FolderIcon } from '@navikt/aksel-icons'

const Ikon = ({ endringstype }: { endringstype: Endringstype }) => {
  if (endringstype == Endringstype.ENDRE_ENHET) {
    return <Buildings3Icon aria-hidden width="1rem" height="1rem" />
  } else if (endringstype == Endringstype.ENDRE_IDENT) {
    return <BagdeIcon aria-hidden width="1rem" height="1rem" />
  } else if (endringstype == Endringstype.OPPRETT_SAK) {
    return <FolderIcon aria-hidden width="1rem" height="1rem" />
  }
}

const Endringstekst = ({ endring }: { endring: ISaksendring }) => {
  if (endring.endringstype == Endringstype.ENDRE_ENHET) {
    return (
      <BodyShort size="small">
        Fra {tekstEnhet[endring.foer.enhet]} til {tekstEnhet[endring.etter.enhet]}
      </BodyShort>
    )
  } else if (endring.endringstype == Endringstype.ENDRE_IDENT) {
    return (
      <BodyShort size="small">
        Identitetsnummer {tekstEnhet[endring.foer.ident]} er erstattet med {tekstEnhet[endring.etter.ident]}
      </BodyShort>
    )
  }
}

export const Sakshistorikk = ({ sakId }: { sakId: number }) => {
  const [hentSaksendringerStatus, hentSaksendringerKall] = useApiCall(hentSaksendringer)

  useEffect(() => {
    hentSaksendringerKall(sakId)
  }, [])

  return mapResult(hentSaksendringerStatus, {
    pending: <Spinner label="Henter historikk..." />,
    success: (sakshistorikk) => {
      return (
        <List as="ul" size="small">
          {sakshistorikk.map((endring) => {
            return (
              <List.Item
                title={tekstEndringstype[endring.endringstype]}
                icon={<Ikon endringstype={endring.endringstype} />}
                key={endring.id}
              >
                <Box maxWidth="18rem" borderWidth="0 0 1 0" paddingBlock="0 2" borderColor="border-subtle">
                  <Endringstekst endring={endring} />
                  {endring.kommentar && (
                    <Box marginBlock="3 3">
                      <BodyShort size="small">{endring.kommentar}</BodyShort>
                    </Box>
                  )}
                  <Detail textColor="subtle">{formaterDatoMedKlokkeslett(endring.tidspunkt)}</Detail>
                  <Detail textColor="subtle">
                    {endring.identtype == Identtype.SAKSBEHANDLER ? endring.ident : 'Gjenny (automatisk)'}
                  </Detail>
                </Box>
              </List.Item>
            )
          })}
        </List>
      )
    },
  })
}
