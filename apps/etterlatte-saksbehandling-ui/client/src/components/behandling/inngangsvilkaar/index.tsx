import styled from "styled-components";
import { Vilkaar } from "./vilkaar";

export const Inngangsvilkaar = () => {
    return (
        <VilkaarContainer>
            <h1>inngangsvilkaar</h1>
            <Vilkaar />
            <Vilkaar />
            <Vilkaar />
            <Vilkaar />
            <Vilkaar />
        </VilkaarContainer>
    );
};

const VilkaarContainer = styled.div`
    margin: 0;
    padding: 0;
`;