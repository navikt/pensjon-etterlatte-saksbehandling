import { Alert, Button, Heading, Panel } from '@navikt/ds-react'
import { IBrev, Mottaker } from '~shared/types/Brev'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { InfoWrapper } from '~components/behandling/soeknadsoversikt/styled'
import React, { Dispatch, SetStateAction, useState } from 'react'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { KildePersondata } from '~shared/types/kilde'
import { BrevMottakerModal } from '~components/person/brev/mottaker/BrevMottakerModal'
import { FlexRow } from '~shared/styled'
import { DocPencilIcon } from '@navikt/aksel-icons'

interface Props {
  vedtaksbrev: IBrev
  setVedtaksbrev: Dispatch<SetStateAction<IBrev>>
  redigerbar: Boolean
  vergeadresse: Grunnlagsopplysning<Mottaker, KildePersondata> | undefined
}

export default function MottakerPanel({ vedtaksbrev, setVedtaksbrev, redigerbar, vergeadresse }: Props) {
  const [erModalAapen, setErModalAapen] = useState<boolean>(false)

  const soekerFnr = vedtaksbrev.soekerFnr

  const mottaker = vedtaksbrev.mottaker
  const adresse = mottaker.adresse

  const soekerErIkkeMottaker = soekerFnr !== mottaker.foedselsnummer?.value

  return (
    <>
      <Panel border>
        <FlexRow justify="space-between">
          <Heading spacing level="2" size="medium">
            Mottaker
          </Heading>
          <div>
            {redigerbar && (
              <Button
                variant="secondary"
                onClick={() => setErModalAapen(true)}
                icon={<DocPencilIcon aria-hidden />}
                size="small"
              />
            )}
          </div>
        </FlexRow>

        {soekerErIkkeMottaker && (
          <Alert variant="info" size="small" inline>
            Søker er ikke mottaker av brevet
          </Alert>
        )}
        {vergeadresse && (
          <Alert variant="info" size="small" inline>
            Søker har verge
          </Alert>
        )}
        <br />

        <InfoWrapper>
          <Info
            wide
            label="Navn"
            tekst={
              /[a-zA-Z\s]/.test(mottaker.navn) ? (
                mottaker.navn
              ) : (
                <Alert variant="error" size="small" inline>
                  Navn mangler
                </Alert>
              )
            }
          />
          {mottaker.foedselsnummer && <Info label="Fødselsnummer" tekst={mottaker.foedselsnummer.value} wide />}
          {mottaker.orgnummer && <Info label="Org.nr." tekst={mottaker.orgnummer} wide />}

          <Info
            wide
            label="Adresse"
            tekst={
              <>
                {!adresse?.adresselinje1 && !adresse?.adresselinje2 && !adresse?.adresselinje3 ? (
                  <Alert variant="warning" size="small" inline>
                    Adresselinjer mangler
                  </Alert>
                ) : (
                  [adresse?.adresselinje1, adresse?.adresselinje2, adresse?.adresselinje3]
                    .filter((linje) => !!linje)
                    .map((linje) => (
                      <>
                        {linje}
                        <br />
                      </>
                    ))
                )}
              </>
            }
          />

          <Info
            wide
            label="Postnummer-/sted"
            tekst={
              !adresse?.postnummer && !adresse?.poststed ? (
                <Alert variant="warning" size="small" inline>
                  Postnummer og -sted mangler
                </Alert>
              ) : (
                <>
                  {adresse?.postnummer} {adresse?.poststed}
                </>
              )
            }
          />

          <Info
            wide
            label="Land"
            tekst={
              <>
                {adresse?.land || (
                  <Alert variant="error" size="small" inline>
                    Land mangler
                  </Alert>
                )}
                {!!adresse?.landkode ? (
                  `(${adresse.landkode})`
                ) : (
                  <Alert variant="error" size="small" inline>
                    Landkode mangler
                  </Alert>
                )}
              </>
            }
          />
        </InfoWrapper>
      </Panel>
      <BrevMottakerModal
        brev={vedtaksbrev}
        setBrev={setVedtaksbrev}
        vergeadresse={vergeadresse}
        isOpen={erModalAapen}
        setIsOpen={setErModalAapen}
      />
    </>
  )
}
