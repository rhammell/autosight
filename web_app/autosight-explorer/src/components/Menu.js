import { Flex } from '@chakra-ui/react'

import MenuHeader from './MenuHeader'
import MenuLoading from './MenuLoading'
import MenuError from './MenuError'
import ImageRecords from './ImageRecords'

const Menu = ({ imageRecords, isLoading, isError, selectedImageRecord, handleSelect }) => {
  return (
    <Flex
      width={'450px'}
      height={'94vh'}
      position='absolute'
      top={'3vh'}
      left={'60px'}
      zIndex={1000}
      backgroundColor={'gray.100'}
      boxShadow={'base'}
      rounded={'sm'}
      borderStyle={'solid'}
      borderWidth={'1px'}
      borderColor={'gray.300'}
      direction={'column'}
    >
      <MenuHeader />
      {isError && <MenuError />}
      {isLoading && <MenuLoading />}
      {!isLoading && !isError && (
        <ImageRecords
          imageRecords={imageRecords}
          selectedImageRecord={selectedImageRecord}
          handleSelect={handleSelect}
        />
      )}
    </Flex>
  );
};

export default Menu;