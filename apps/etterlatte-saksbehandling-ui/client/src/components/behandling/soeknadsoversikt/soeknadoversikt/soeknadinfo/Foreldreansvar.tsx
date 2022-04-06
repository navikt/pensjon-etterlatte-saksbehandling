import { Label } from '@navikt/ds-react'
import { DetailWrapper, WarningIconWrapper } from '../../styled'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'
import { IPersonOpplysningFraPdl } from '../../../types'
import { VurderingsResultat, IGyldighetproving } from '../../../../../store/reducers/BehandlingReducer'

export const Foreldreansvar = ({
  gjenlevendePdl,
  gjenlevendeHarForeldreansvar,
}: {
  gjenlevendePdl: IPersonOpplysningFraPdl
  gjenlevendeHarForeldreansvar: IGyldighetproving | undefined
}) => {
  return (
    <DetailWrapper>
      {gjenlevendeHarForeldreansvar?.resultat === VurderingsResultat.OPPFYLT && (
        <div>
          <Label size="small">Foreldreansvar</Label>
          {gjenlevendePdl?.fornavn} {gjenlevendePdl?.etternavn}
          <div>(gjenlevende forelder)</div>
        </div>
      )}

      {gjenlevendeHarForeldreansvar?.resultat === VurderingsResultat.IKKE_OPPFYLT && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Foreldreansvar
          </Label>
          <span className="warningText">
            {gjenlevendePdl?.fornavn} {gjenlevendePdl?.etternavn}
          </span>
        </div>
      )}

      {gjenlevendeHarForeldreansvar?.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Foreldreansvar
          </Label>
          <span className="warningText">Mangler info</span>
        </div>
      )}
    </DetailWrapper>
  )
}
