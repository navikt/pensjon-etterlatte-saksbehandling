import styled from "styled-components";

export const Checkmark = () => {
    return (
        <Circle>
            <svg
                width="15px"
                height="15px"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                focusable="false"
                role="img"
            >
                <path
                    fillRule="evenodd"
                    clipRule="evenodd"
                    d="M8.028 16L20.5 4 22 5.5 8.028 19 2 13l1.5-1.5L8.028 16z"
                    fill="#fff"
                ></path>
            </svg>
        </Circle>
    );
};

const Circle = styled.div`
    background-color: #1c6937;
    border-radius: 100%;
    height: 20px;
    width: 20px;
    display: flex;
    justify-content: center;
    align-items: center;
`;
