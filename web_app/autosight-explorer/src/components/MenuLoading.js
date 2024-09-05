import MoonLoader from "react-spinners/MoonLoader";

import {
  Box,
  Center,
  Flex
} from '@chakra-ui/react'

const MenuLoading = () => {
  return (
    <Center flex='1'>
      <MoonLoader color="blue" />
    </Center>
  );
};

export default MenuLoading;