import {
  Flex,
  Spacer,
  Box,
  Text,
  HStack,
  VStack,
  Icon,
  Image,
  Skeleton
} from '@chakra-ui/react'
import { BsClock, BsGeoAlt } from "react-icons/bs";
import scrollIntoView from 'scroll-into-view';
import { useEffect, useRef, useState} from 'react';
import { useInView } from 'react-intersection-observer';

const ImageRecord = ({imageRecord, isSelected, handleSelect }) => {
  const elRef = useRef(null)
  const [imageLoaded, setImageLoaded] = useState(false);
  const [ref, inView] = useInView({
    triggerOnce: true,
    rootMargin: '200px 0px',
  });

  function epochToDateString(epoch) {
    const date = new Date(epoch);
    
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const year = date.getFullYear();
    
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');
    
    return `${month}/${day}/${year} ${hours}:${minutes}:${seconds}`;
  }

  // Effect called when isSelected changes
  useEffect(() => {
    if (isSelected) {
      scrollIntoView(elRef.current, {
        time: 2000,
        align:{top: 0, topOffset: 7}
      })
    }
  }, [isSelected])

  return (
    <Box
      ref={elRef}
      backgroundColor={'white'}
      borderWidth={'1px'}
      borderStyle={'solid'}
      borderColor={isSelected ? 'gray.600' : 'gray.200'}
      boxShadow={isSelected ? 'md' : 'sm'}
      w={'100%'}
      rounded="sm"
      onClick={() => handleSelect(imageRecord.id)}
      cursor='pointer'
    >
      <Box ref={ref}>
        <Skeleton isLoaded={imageLoaded} height={imageLoaded ? 'auto' : '320px'}>
          {inView && (
            <Image
              src={imageRecord.imageURL}
              onLoad={() => setImageLoaded(true)}
              objectFit='cover'
              w='100%'
              h='auto'
            />
          )}
        </Skeleton>
      </Box>
      
      <Flex 
        p={2}
        fontWeight={'bold'}
      >
        
        <Box textAlign={'end'}>
          <VStack>
            <HStack>
              <Text fontSize='sm' color='gray.600'>
                {epochToDateString(imageRecord.captureTime)}
              </Text>
            </HStack>
          </VStack>
        </Box>
        <Spacer />
        <Box>
        <VStack>
            <HStack>
              <Text fontSize='sm' color='gray.600'>
               {imageRecord.latitude}, {imageRecord.longitude}
              </Text>
            </HStack>
          </VStack>
        </Box>
        
      </Flex>
    </Box>
  );
};

export default ImageRecord;