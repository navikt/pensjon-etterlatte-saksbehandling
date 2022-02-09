import styled from "styled-components";
import { Content } from "../../../shared/styled";
import { Status, VilkaarStatus, VilkaarType } from "./types";
import { Vilkaar } from "./vilkaar";

export const Inngangsvilkaar = () => {
    return (
        <Content>
            <VilkaarListe>
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.DONE,
                        vilkaarType: VilkaarType.doedsdato,
                        vilkaarStatus: VilkaarStatus.OPPFYLT,
                    }}
                />
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.NOT_DONE,
                        vilkaarType: VilkaarType.alderBarn,
                        vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
                    }}
                />
            </VilkaarListe>
        </Content>
    );
};

const VilkaarListe = styled.div`
    padding: 0 2em;
`;
