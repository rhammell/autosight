import {
  Box,
  VStack
} from '@chakra-ui/react'

import ImageRecord from './ImageRecord'

const ImageRecords = ({ imageRecords, selectedImageRecord, handleSelect }) => {
  return (
      <Box
        flex={1}
        overflowY={'scroll'}  
        p={2}
      >
        <VStack
          h={'100%'}
          pb={2}
          mb={4}
        >
          {imageRecords.map(imageRecord => 
            <ImageRecord
              imageRecord={imageRecord} 
              key={imageRecord.id} 
              isSelected={selectedImageRecord == imageRecord.id} 
              handleSelect={handleSelect}/>
            )}
        </VStack>
      </Box>
  );
};

export default ImageRecords;